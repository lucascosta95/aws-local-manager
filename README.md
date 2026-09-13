🇧🇷 [Versão em Português](README.pt-BR.md)

# AWS Local Manager

![License](https://img.shields.io/github/license/lucascosta95/aws-local-manager)
![Release](https://img.shields.io/github/v/release/lucascosta95/aws-local-manager)
![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Linux-lightgrey)
![Built with Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)

Desktop GUI for managing local AWS emulator services — built for backend developers who work with AWS locally.

---

## 🧩 The Problem

Working with a local AWS emulator (like [Floci](https://hub.docker.com/r/hectorvent/floci)) means running AWS CLI commands or Terraform manually for every operation: creating queues, publishing messages, checking service health, destroying resources. It's slow, error-prone, and breaks the development flow.

## 💡 Solution

AWS Local Manager provides a visual interface that integrates directly with your Terraform projects and the Floci emulator. Create and destroy resources from your existing `.tf` files, publish messages, monitor service health — all from a single window.

---

## ✨ Features

- 🩺 **Real-time health dashboard** — monitor all emulated AWS services at a glance, with configurable polling interval
- 🏗️ **Infrastructure from Terraform** — read your `.tf` files and provision resources directly into the emulator without running `terraform apply`
- ⚡ **Quick Create** — spin up SQS queues, SNS topics, S3 buckets, DynamoDB tables, and SSM parameters without Terraform
- 📤 **Message publishing** — send JSON messages to SQS, SNS, DynamoDB, and Step Functions; upload files to S3
- 🔁 **Step Functions execution** — trigger state machine executions with custom JSON input
- 💾 **Saved payloads** — store and reuse common message payloads per project via `payloads.json`
- 🌍 **i18n** — interface available in English and Portuguese (pt-BR)
- 🎨 **Light and dark theme**
- 🔍 **Inspector** — browse and inspect the content of SQS queues, Step Functions executions, DynamoDB tables, S3 buckets, ElastiCache keys, and SSM parameters directly from the app
- 🔄 **Auto-update** via GitHub Releases

**Supported services:** SQS · SNS · S3 · DynamoDB · Step Functions · ElastiCache · SSM Parameter Store

---

## 📋 Prerequisites

| Dependency | Purpose |
|---|---|
| JDK 17+ | Runtime |
| Docker | Runs the AWS emulator container |
| AWS CLI | Used internally to provision resources |

Pull the emulator image before first launch:

```bash
docker pull floci/floci:2.0.1
```

> The Setup screen checks all prerequisites on launch and can auto-fix most issues.

The app looks for `docker`, `colima` and `aws` on `PATH` and, failing that, in the usual install
locations (`/opt/homebrew/bin`, `/usr/local/bin`, `~/.docker/bin`, `~/.rd/bin`, among others). This
matters on macOS: an app launched from Finder is started by launchd with a bare
`/usr/bin:/bin:/usr/sbin:/sbin` and never reads your shell profile, so a Homebrew install is
invisible to it even though the same tools work in a terminal. The Logs screen records where each
tool was found, or that it was not found at all.

Each release is pinned to one emulator version. When you upgrade from a release that used an older
one, Setup reports both the image and the emulator as **Outdated**: Docker keeps the old image and
keeps serving it to the container already created from it, so a plain `docker pull` is not enough.
Fixing the image check downloads the supported version, removes the container the app created from
the old image, and deletes the old image. Fixing the emulator check then recreates the container on
the supported version. Everything removed is named in the fix log.

---

## 📦 Installation

### macOS / Linux — via script (no formal installation)

Ideal for environments where installing applications is not possible.
Requires only **Java 17+** and **curl**.

**1. Install the script once:**
```bash
curl -fsSL https://raw.githubusercontent.com/lucascosta95/aws-local-manager/main/scripts/awslocal \
  -o ~/.local/bin/awslocal && chmod +x ~/.local/bin/awslocal
```

**2. Run from any terminal:**
```bash
awslocal
```

The script automatically checks for a new version on each run and updates the cache at `~/.aws-local-manager/` when needed.

---

### macOS — native installer (.dmg)

Download the `.dmg` from [GitHub Releases](https://github.com/lucascosta95/aws-local-manager/releases/latest) and drag to Applications.

### Linux — native installer (.deb)

Download the `.deb` from [GitHub Releases](https://github.com/lucascosta95/aws-local-manager/releases/latest), then run:

```bash
sudo dpkg -i aws-local-manager_*.deb
```

---

## 🚀 Usage

1. **Setup** — on first launch, the app checks Docker, the emulator container, and AWS CLI. Fix any issues with one click.
2. **Dashboard** — verify all services are healthy before starting work.
3. **Infrastructure** — point the app to your projects directory, select a project, and apply its Terraform resources to the emulator.
4. **Running** — browse active resources, publish messages, and delete resources when done.

---

## 📁 Project Setup

AWS Local Manager discovers projects by scanning a directory you configure in **Settings → Projects Directory**. Each project must follow this structure:

```
~/projects/                        ← configured root directory
├── my-api/
│   └── infra/
│       ├── aws-local.config.json  ← project metadata (name)
│       ├── queues.tf
│       ├── topics.tf
│       └── payloads.json          ← saved message payloads (optional)
└── another-service/
    └── infra/
        ├── aws-local.config.json
        └── tables.tf
```

### aws-local.config.json

Identifies the project inside the app. Only `name` is required:

```json
{
  "name": "Nimbus API"
}
```

### Session logs

The **Logs** entry in the side bar shows everything the app did since it started: every external
command it ran with its exit code, every emulator health failure, and every exception it caught,
with the stack trace one click away. It is the only place those exceptions surface — elsewhere the
app reports that something failed without saying what threw.

The buffer lives in memory, is capped at 2000 entries and is never written to disk, so closing the
app discards it. Repeated identical entries collapse into one with a counter, which keeps a polling
failure from burying everything else. Filter by level, by source or by free text over the message
and the stack trace, copy what is visible, or clear it.

The list follows the newest entry only while you are standing at the bottom of it: scrolling up
stops that, scrolling back down resumes it, and the Tail chip jumps to the end. Hovering a line
reveals a button that copies that entry with its stack trace.

The log panels already on the Infrastructure and Setup screens are unchanged; this is a separate
view over everything at once.

### AI agent skill

The **Skills** screen installs the bundled skill into the AI coding tools found in your home
folder. Afterwards you open any project in the tool you like, call the skill, and it sweeps that
project for the AWS services the code already talks to — reading the source, the configuration and
the `.env` files — then writes `infra/` with the correct structure.

Every tool gets the same Agent Skills layout, one folder per skill, so the instructions are loaded
only when you call the skill and never sit in unrelated conversations.

| Tool | Where it lands | How you call it |
|---|---|---|
| Claude Code | `~/.claude/skills/aws-local-infra/SKILL.md` | `/aws-local-infra` |
| Cursor | `~/.cursor/skills/aws-local-infra/SKILL.md` | `/aws-local-infra` |
| Codex CLI | `~/.codex/skills/aws-local-infra/SKILL.md` | `$aws-local-infra` |
| Gemini CLI | `~/.gemini/skills/aws-local-infra/SKILL.md` | picked from its description; `/skills` lists it |

The screen shows every absolute path before writing anything, and the catalog ships inside the app
and is refreshed from GitHub when online. Restart the agent after installing, since all four read
their skills folder at startup.

Releases up to 1.2.0 wrote a Cursor rule into `~/.cursor/rules` and appended a block to
`~/.codex/AGENTS.md` and `~/.gemini/GEMINI.md`. Installing again removes both, keeping a `.bak`
copy of the instruction files.

To install by hand instead, or into a single project rather than globally, swap `.claude` for
`.cursor`, `.codex` or `.gemini`:

```bash
mkdir -p .claude/skills/aws-local-infra && \
curl -fsSL https://raw.githubusercontent.com/lucascosta95/aws-local-manager/main/skills/aws-local-infra/SKILL.md \
  -o .claude/skills/aws-local-infra/SKILL.md
```

The skill is plain Markdown with `name` and `description` frontmatter, so it also works pasted into
`AGENTS.md` or into the prompt of any other assistant.

The skill itself is written in English, because that is what the agent reads. The screen around it
follows the language selected in the app: each entry in `skills/catalog.json` carries a
`translations` map keyed by language tag, and a skill published without a translation falls back to
the English `name` and `description`. **View content** always shows the skill as it is installed.

### Terraform templates

The app never runs `terraform apply`. It reads the `.tf` files with a lightweight parser and calls the AWS CLI against the emulator, so the files can stay minimal — no `provider`, `backend`, IAM, variables or modules are needed.

How the parser reads a file:

- Only `.tf` files placed **directly** inside `infra/` are read; subdirectories are skipped.
- Every resource must be a top-level block: `resource "<aws_type>" "<label>" { ... }`. The label accepts letters, digits and `_` only.
- The AWS name comes from the `name` attribute of the block. When it is absent, the app falls back to the label with `_` replaced by `-`. The exception is `aws_ssm_parameter`, which is skipped when `name` is missing.
- Values must be literal strings. `var.*`, `local.*` and `${...}` interpolations are **not** resolved.
- Any other attribute is ignored by the app and harmless to keep, so the same file still works with real Terraform.

> 💡 On the **Infrastructure** screen, the **Create template** button writes a ready-to-edit file for any of the types below.

#### SQS

```hcl
resource "aws_sqs_queue" "nimbus_queue" {
  name = "nimbus-queue"
}
```

Timing and retry attributes are accepted and kept for real Terraform runs, but the app creates the queue with the emulator defaults:

```hcl
resource "aws_sqs_queue" "nimbus_queue" {
  name                       = "nimbus-queue"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600
  delay_seconds              = 0
  receive_wait_time_seconds  = 0
}
```

A dead-letter queue is just a second queue. The app creates both, but the redrive policy itself is not applied to the emulator — use **Quick Create** when you need the queue wired to a DLQ:

```hcl
resource "aws_sqs_queue" "nimbus_queue_dlq" {
  name = "nimbus-queue-dlq"
}

resource "aws_sqs_queue" "nimbus_queue" {
  name = "nimbus-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.nimbus_queue_dlq.arn
    maxReceiveCount     = 3
  })
}
```

#### SNS

```hcl
resource "aws_sns_topic" "nimbus_topic" {
  name = "nimbus-topic"
}
```

#### SNS subscription

`topic_arn` and `endpoint` may reference another resource declared in the same folder (`aws_sns_topic.<label>.arn`, `aws_sqs_queue.<label>.arn`) or carry a literal ARN. The subscription is applied only when the endpoint resource is part of the selected resources:

```hcl
resource "aws_sns_topic_subscription" "nimbus_topic_to_queue" {
  topic_arn            = aws_sns_topic.nimbus_topic.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.nimbus_queue.arn
  raw_message_delivery = true
}
```

`filter_policy` is supported through `jsonencode`, with one attribute per line and a valid JSON value on each of them (nested objects are not parsed):

```hcl
resource "aws_sns_topic_subscription" "nimbus_topic_to_queue" {
  topic_arn = aws_sns_topic.nimbus_topic.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.nimbus_queue.arn

  filter_policy       = jsonencode({
    eventType = ["created", "updated"]
    priority  = ["high"]
  })
  filter_policy_scope = "MessageAttributes"
}
```

#### S3

The parser looks for `name`, which an `aws_s3_bucket` block does not have, so the bucket name is derived from the label with `_` replaced by `-`. Keep the label and the `bucket` value aligned:

```hcl
resource "aws_s3_bucket" "nimbus_bucket" {
  bucket = "nimbus-bucket"
}
```

#### DynamoDB

The table is always created with a single `id` partition key of type `S` and `PAY_PER_REQUEST` billing, regardless of the keys declared in the file:

```hcl
resource "aws_dynamodb_table" "nimbus_table" {
  name         = "nimbus-table"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}
```

#### Step Functions

Only `name` is used. The state machine is created in the emulator with a single pass-through state, so the `definition` below is kept for real Terraform runs and for documentation:

```hcl
resource "aws_sfn_state_machine" "nimbus_flow" {
  name     = "nimbus-flow"
  role_arn = "arn:aws:iam::000000000000:role/stepfunctions-role"

  definition = jsonencode({
    Comment = "nimbus-flow",
    StartAt = "HelloWorld",
    States  = {
      HelloWorld = { Type = "Pass", End = true }
    }
  })
}
```

#### ElastiCache

The name comes from `cluster_id`. With `engine = "redis"` the app creates a replication group; with `engine = "memcached"` it creates a cache cluster using `num_cache_nodes`:

```hcl
resource "aws_elasticache_cluster" "nimbus_cache" {
  cluster_id      = "nimbus-cache"
  engine          = "redis"
  node_type       = "cache.t3.micro"
  num_cache_nodes = 1
  port            = 6379
}
```

The parser only reads quoted values, so an unquoted `num_cache_nodes = 1` falls back to the default of a single node — write it as `num_cache_nodes = "2"` when a Memcached cluster needs more. `port` is never sent to the emulator: Redis answers on `6379` and Memcached on `11211`.

#### SSM Parameter Store

`name` is required for this type. A Terraform block label cannot contain a slash, so the usual fallback to the label would invent a wrong parameter name — a block without `name` is skipped by the app instead.

```hcl
resource "aws_ssm_parameter" "nimbus_db_host" {
  name  = "/nimbus/db/host"
  type  = "String"
  value = "localhost"
}
```

The app sends `name`, `value` and `type` to the emulator with `put-parameter --overwrite`, so creating a parameter that already exists replaces its value and bumps the version. A missing `value` is sent as an empty string and a missing `type` falls back to `String`.

> ⚠️ A `SecureString` value written into a `.tf` file is a secret stored in plain text in your repository, and the Inspector shows it decrypted. Keep local debugging on `String`.

#### What the app reads from each type

| Terraform type | Attributes used | Created in the emulator as |
|---|---|---|
| `aws_sqs_queue` | `name` | Queue with emulator defaults |
| `aws_sns_topic` | `name` | Topic |
| `aws_sns_topic_subscription` | `topic_arn`, `endpoint`, `protocol`, `raw_message_delivery`, `filter_policy`, `filter_policy_scope` | Subscription |
| `aws_s3_bucket` | block label | Bucket |
| `aws_dynamodb_table` | `name` | Table with `id` (`S`) partition key, `PAY_PER_REQUEST` |
| `aws_sfn_state_machine` | `name` | State machine with a single `Pass` state |
| `aws_elasticache_cluster` | `cluster_id`, `engine`, `node_type`, `num_cache_nodes` (quoted) | Replication group (redis) or cache cluster (memcached) |
| `aws_ssm_parameter` | `name` (required), `value`, `type` | Parameter written with `put-parameter --overwrite` |

### payloads.json

Store message payloads alongside your infra files and reuse them from the Running screen:

```json
[
  {
    "name": "Order created",
    "queue": "orders-queue",
    "payload": {
      "orderId": "abc-123",
      "customerId": "usr-456",
      "status": "created",
      "items": [
        { "sku": "PROD-01", "qty": 2 }
      ]
    }
  },
  {
    "name": "Payment approved",
    "topic": "payments-topic",
    "payload": {
      "orderId": "abc-123",
      "amount": 99.90,
      "currency": "USD"
    }
  }
]
```

---

## 🛠️ Development

```bash
git clone https://github.com/lucascosta95/aws-local-manager.git
cd aws-local-manager
./gradlew :desktop:run
```

Build native packages:

```bash
# Linux (.deb)
./gradlew :desktop:packageDeb

# macOS (.dmg)
./gradlew :desktop:packageDmg
```

Output is placed in `desktop/build/compose/binaries/`. Each package has to be built on the system it
targets, since `jpackage` only produces the format of the machine it runs on.

The macOS bundle takes its icon from `desktop/icons/icon.icns`, because `jpackage` reads only
`.icns` there and ignores a `.png` without reporting anything. After changing
`desktop/src/desktopMain/resources/icon.png`, regenerate it on a Mac:

```bash
./scripts/generate_icns.sh
```

The `.dmg` build fails if that file is missing or is not a real `.icns`, rather than quietly
shipping the default Java icon. Linux keeps using the `.png` directly, which `.deb` accepts.

---

## 📄 License

[MIT](LICENSE)
