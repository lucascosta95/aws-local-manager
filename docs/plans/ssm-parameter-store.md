# Implementation prompt: AWS SSM Parameter Store support

Hand this file to a coding agent running locally, where Docker and the AWS CLI are
available. It is written to be executed top to bottom.

## Goal

Add SSM Parameter Store as a supported service in AWS Local Manager: read
`aws_ssm_parameter` blocks from a project's Terraform files, create and delete the
parameters in the Floci emulator, create them ad hoc from Quick Create, browse them in
the Inspector, and document the template in both READMEs and in the bundled skill.

## Context you need before touching code

The app never runs `terraform apply`. `TerraformReader` parses `.tf` files with regular
expressions and each supported service turns that into AWS CLI calls. Every service
implements `AwsResourceDefinition`
(`desktop/src/desktopMain/kotlin/dev/lucascosta/awslocalmanager/data/model/aws/AwsResourceDefinition.kt`)
and registers itself in `ResourceRegistry` from `App.kt`.

Two things already work in your favour:

- `AwsServiceType.SSM` already exists in `domain/AwsServiceType.kt` with the Systems
  Manager icon, so the dashboard can already render the service.
- Step Functions and ElastiCache talk to the emulator through the AWS CLI via
  `ProcessRunner`, not through the Kotlin SDK. Follow that path and **no new Gradle
  dependency is needed**.

One thing is genuinely new. Every currently supported resource is identified by a name
alone. A parameter also carries a **value** and a **type**. The `extraProperties` map on
`TerraformResource` already exists for this purpose (ElastiCache uses it), so carry
`value` and `type` there.

Floci supports Parameter Store with version history, labels, SecureString and tagging.
The emulator image is pinned to `floci/floci:2.0.1` in `constants/AppConstants.kt`.

## Phase 1: validate the emulator first, before writing code

Do not skip this. It decides how the listing works in phases 2 and 4.

```bash
docker run --rm -d -p 4566:4566 --name floci-ssm-check floci/floci:2.0.1

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

aws ssm put-parameter --name /nimbus/db/host --value localhost --type String
aws ssm put-parameter --name /nimbus/db/pool --value "10,20" --type StringList
aws ssm describe-parameters
aws ssm get-parameter --name /nimbus/db/host
aws ssm get-parameters-by-path --path /nimbus --recursive
aws ssm delete-parameter --name /nimbus/db/host

docker rm -f floci-ssm-check
```

Those credentials and that endpoint are exactly what `ProcessRunner.awsEnvVars` sets, so
whatever works here works from the app.

Record two answers before continuing:

1. Does `describe-parameters` return the names? That is the health probe and the running
   list.
2. Does `get-parameters-by-path --recursive` return names **and** values in one call? If
   yes, the Inspector reads a whole tree in one call. If no, it needs one `get-parameter`
   per item and should paginate.

If Parameter Store turns out to be missing or broken on 2.0.1, stop and report that
instead of working around it.

## Phase 2: domain and integration

New files:

- `data/model/resources/SsmParameterResource.kt` implementing `AwsResourceDefinition`.
  Copy the shape from `data/model/resources/ElastiCacheResource.kt`, which is the only
  other service that reads extra properties. Use `terraformPrefix = "aws_ssm_parameter"`,
  `healthKey = "ssm"`, `isQuickCreatable = true`, `hasFilePublish = false`,
  `publishableViaJson = false`, `supportsPayloads = false`. `createCommand` reads `value`
  and `type` from `extraProperties`, defaulting the type to `String`.
- `data/remote/AwsSsmClient.kt` for listing and reading. Copy
  `data/remote/AwsStepFunctionsClient.kt`, which runs the CLI through `ProcessRunner`.
- `domain/SsmHealthProbe.kt`. Copy `domain/ElastiCacheHealthProbe.kt` and call
  `probeResources { ... }` so empty means AVAILABLE and non-empty means ACTIVE.

Edited files:

- `data/remote/AwsCommands.kt`: add `createSsmParameter(name, value, type)` using
  `put-parameter --overwrite` and `deleteSsmParameter(name)`.
- `data/repository/RunningResourceRepository.kt`: add the client factory to the
  constructor, an `async` branch in `fetchAllRunningResources`, and the delete branch.
- `di/DataModule.kt` and `di/DomainModule.kt`: register the client factory and the probe.
- `App.kt`: add the resource to the `ResourceRegistry.register(...)` call.

## Phase 3: Terraform reading

Edit `domain/TerraformReader.kt`, in `parseResourcesFromFile`. It currently special cases
`aws_elasticache_cluster` to pull extra attributes. Add the same treatment for
`aws_ssm_parameter`, extracting `value` and `type` into `extraProperties`.

Two rules that must hold:

- The existing `namePattern` already picks up `name`, so the AWS name comes for free.
- Parameter names are paths such as `/nimbus/db/host`, and a Terraform block label cannot
  contain a slash. The fallback that derives the name from the label is therefore wrong
  for this type: treat a missing `name` as an unusable resource rather than inventing one.
- `extractQuotedAttribute` only matches quoted values, so an unquoted number or boolean is
  not read. Parameter values are strings, so this is fine, but keep it in mind.

## Phase 4: screens

- **Quick Create** (`features/quick/`): add a value field and a type selector. The engine
  selector for ElastiCache and the key type for DynamoDB are the precedents, in
  `QuickScreen.kt`, `QuickUiState.kt` and `QuickViewModel.kt`.
- **Inspector** (`features/inspector/handler/`): add `SsmInspectorHandler` following
  `ElastiCacheInspectorHandler.kt`, and register it in `App.kt` next to the other
  `InspectorHandlerRegistry.register(...)` calls. Use whichever listing phase 1 proved.
  Inspector labels go in `i18n/InspectorStrings.kt`, which has its own data class and
  CompositionLocal.
- **Running screen**: nothing to do. A parameter accepts no publish action, and
  ElastiCache already proves that path.

## Phase 5: templates and skill

Add the template to both READMEs, in the Terraform section: a new `#### SSM Parameter
Store` subsection and a row in the "What the app reads from each type" table.

```hcl
resource "aws_ssm_parameter" "nimbus_db_host" {
  name  = "/nimbus/db/host"
  type  = "String"
  value = "localhost"
}
```

Say plainly in the docs that `name` is required for this type, that the app sends `value`
and `type` to the emulator, and that a `SecureString` value written into a `.tf` file is a
secret in plain text in the user's repository. Steer local debugging to `String`.

Then update the skill at `skills/aws-local-infra/SKILL.md` with the same template and the
same warning, and add SSM to its summary table.

**Do not forget the catalog guard.** Editing `SKILL.md` changes its hash, and
`skills/catalog.json` pins a `sha256`. Bump the skill `version` in the catalog and update
the hash, otherwise the build fails on the `verifySkillCatalog` task. The failure message
prints the correct hash to paste:

```bash
sha256sum skills/aws-local-infra/SKILL.md
```

## Constraints

- Work on a branch, never commit straight to `main`.
- Conventional commits, one line, no trailers and no `Co-Authored-By`.
- Before every commit run what CI runs:

```bash
./gradlew ktlintFormat ktlintCheck detekt :desktop:assemble
```

- ktlint caps lines at 140 characters and cannot auto-fix a long string literal. detekt
  caps at 160. Write to 140.
- Do not add Gradle dependencies. The AWS CLI path needs none.
- Main UI labels live in three files that must stay in sync: `i18n/Strings.kt`,
  `StringsEnUs.kt` and `StringsPtBr.kt`. A large group of new labels should get its own
  data class and CompositionLocal, as `InspectorStrings.kt` and `SkillsStrings.kt` do.

## Definition of done

- [ ] Phase 1 answers recorded, with the real CLI output.
- [ ] A parameter declared in a `.tf` file is created in the emulator from the
      Infrastructure screen, and deleting it from Running removes it.
- [ ] Quick Create makes a parameter with a name, a value and a type.
- [ ] The Inspector lists parameters and shows values.
- [ ] The dashboard shows SSM with the Systems Manager icon and a correct status.
- [ ] Both READMEs carry the template, the attribute table row and the secret warning.
- [ ] The skill carries the template, and `catalog.json` has the bumped version and the
      matching hash.
- [ ] `./gradlew ktlintCheck detekt :desktop:assemble` is green.
