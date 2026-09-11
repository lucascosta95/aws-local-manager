---
name: aws-local-infra
description: Sweep the open project for the AWS services it already uses, reading its code, configuration and .env files, then create or repair the infra/ folder that AWS Local Manager reads so the project can be debugged against a local AWS emulator. Use when the user wants to run or debug their service against local SQS, SNS, S3, DynamoDB, Step Functions, ElastiCache or SSM Parameter Store, mentions AWS Local Manager or Floci, or asks to create, fix or extend infra/, aws-local.config.json, the .tf templates or payloads.json.
---

# AWS Local Manager infrastructure

AWS Local Manager is a desktop app that reads `.tf` files from a project and provisions
the resources into a local AWS emulator through the AWS CLI. It never runs
`terraform apply`, so the files it reads are deliberately minimal.

This skill sets up the folder that app expects, inside the user's own project. Call it from the
project you want to debug: it sweeps that project for the AWS services the code already talks to
and writes `infra/` from what it finds, instead of asking the user to type the names.

## Discovery rules that must not be broken

The app scans a root directory the user configures in **Settings → Projects Directory**.
Its parser is regex based, not Terraform, and it fails silently. Get these wrong and the
project simply never shows up in the app, with no error message.

1. The project must be an **immediate child** of the configured root directory. A project
   at `~/projects/apps/my-api` is not discovered when the root is `~/projects`.
2. `infra/` must sit at the **project root**: `<project>/infra/`.
3. `infra/aws-local.config.json` must exist and be valid JSON.
4. At least **one parsable resource** must exist in a `.tf` file. A folder with only the
   config file is discarded and the project disappears from the list.
5. Only `.tf` files placed **directly** in `infra/` are read. Subdirectories are skipped.
6. Directories whose name starts with `.` are skipped.

## Step 1: locate the project

Work in the repository root the user is currently in. If `infra/` already exists, read
what is there and extend it instead of overwriting.

Tell the user the absolute path of the project's **parent** directory at the end, because
that is the value they must set in Settings → Projects Directory.

## Step 2: sweep the project for the AWS services it uses

Do not ask the user which queues and buckets to create. Read them from the project. The names have
to be the ones the running application already asks for, otherwise the service starts and finds
nothing.

### 2.1 Read the configuration first

Configuration holds the real names more often than the source does. List what exists, then **open
and read** the files, do not only grep them.

```bash
find . -maxdepth 4 -type f \
  \( -name '.env*' -o -name 'application*.y*ml' -o -name 'application*.properties' \
     -o -name 'docker-compose*.y*ml' -o -name 'serverless.y*ml' -o -name 'template.y*ml' \
     -o -name 'appsettings*.json' -o -name 'settings.py' -o -name '*.tf' \) \
  -not -path '*/node_modules/*' -not -path '*/build/*' -not -path '*/target/*' -not -path '*/dist/*'
```

`.env` files are listed in `.gitignore` almost everywhere, and `rg` skips ignored files by default,
so a plain `rg` sweep never sees them. Read them with `cat`, and pass `--no-ignore --hidden` to any
`rg` command. Take the resource names out of them and nothing else: a password, token or access key
read here never goes into `infra/`.

### 2.2 Sweep the source

```bash
rg -n -i --no-ignore --hidden -g '!{.git,node_modules,build,target,dist,.gradle,vendor}/**' \
  -e 'sqs|sns|s3|dynamo|stepfunction|sfn|state.?machine|elasticache|redis|memcached|ssm|parameter.?store|localstack'

rg -n -i --no-ignore --hidden -g '!{.git,node_modules,build,target,dist,.gradle,vendor}/**' \
  -e '(queue|topic|bucket|table|cluster|parameter)[_-]?(name|url|arn|id|path)?\s*[:=]'
```

`rg` is not installed everywhere. `grep` takes the same patterns:

```bash
grep -rIn -E -i 'sqs|sns|s3|dynamo|sfn|elasticache|redis|memcached|ssm' . \
  --exclude-dir={.git,node_modules,build,target,dist,.gradle,vendor}
```

Read the whole output. Do not pipe it through `head`: the resource that gets cut off is the one
the user notices missing. Narrow the pattern per service instead when a project is large.

Good places to look, by stack:

| Stack | Where the names usually live |
|---|---|
| Java / Kotlin | `@SqsListener`, `SqsClient`, `SnsClient`, `S3Client`, `DynamoDbClient`, `application*.yml` |
| Node / TypeScript | `@aws-sdk/client-*`, `QueueUrl`, `TopicArn`, `Bucket`, `TableName` |
| Python | `boto3.client("sqs")`, `queue_url`, `topic_arn`, `table_name` |
| Go | `sqs.New`, `sns.New`, `s3.New`, `dynamodb.New`, `QueueUrl`, `TableName` |
| .NET | `IAmazonSQS`, `IAmazonS3`, `appsettings*.json` |
| Any | `.env*`, `docker-compose*.yml`, `serverless.yml`, `template.yaml`, Helm values, `*_QUEUE`, `*_TOPIC`, `*_BUCKET`, `*_TABLE`, `*_PARAMETER` |

An existing `terraform/` folder, a `serverless.yml` or a SAM `template.yaml` is the best source of
all: it is the real deployed infrastructure. Take the names from it, but do not copy the files into
`infra/` — the app's parser reads none of that, and Step 4 explains what it does read.

### 2.3 Turn the findings into a resource list

| What you found | What to create |
|---|---|
| `SqsClient`, `@SqsListener`, `QueueUrl`, `*_QUEUE*` | `aws_sqs_queue` |
| a queue named `*-dlq`, or a `redrive_policy` | a second `aws_sqs_queue` |
| `SnsClient`, `TopicArn`, `*_TOPIC*` | `aws_sns_topic` |
| a topic that fans out into a queue the app consumes | `aws_sns_topic_subscription` |
| `S3Client`, `Bucket`, `*_BUCKET*` | `aws_s3_bucket` |
| `DynamoDbClient`, `TableName`, `*_TABLE*` | `aws_dynamodb_table` |
| `sfn`, `StateMachineArn`, `startExecution` | `aws_sfn_state_machine` |
| `redis`, `memcached`, a Lettuce / Jedis / ioredis client | `aws_elasticache_cluster` |
| `ssm`, `getParameter`, a `/path/like/this` config key | `aws_ssm_parameter` |

Only the seven types above exist in AWS Local Manager. When the project uses something else, such
as Kinesis, EventBridge or Secrets Manager, say so plainly and leave it out instead of inventing a
resource the app cannot create.

When a name is assembled at runtime, as in `QUEUE=${ENV}-orders`, resolve it with the values the
local profile uses and create the resolved name. State the assumption when you confirm the list.

### 2.4 Confirm before writing

Send one message with the resources you are about to create, each with the file and line the name
came from. Write the files after the user agrees.

## Step 3: write aws-local.config.json

Only `name` is read. Unknown keys are ignored.

```json
{
  "name": "Orders API"
}
```

## Step 4: write the .tf files

Group resources by kind, one file per kind: `queues.tf`, `topics.tf`, `buckets.tf`,
`tables.tf`, `parameters.tf`. Keep them minimal. Do **not** add `provider`, `backend`,
`terraform`, IAM, tags, modules or variables. The parser resolves nothing: `var.*`,
`local.*` and `${...}` are never expanded, so every value must be a literal string.

Block labels accept letters, digits and `_` only. No hyphens, no dots.

### SQS

The AWS name comes from `name`. Timing and retry attributes are valid Terraform and are
kept, but the app creates the queue with the emulator defaults.

```hcl
resource "aws_sqs_queue" "orders_events" {
  name = "orders-events"
}
```

A dead-letter queue is a second queue. Both are created, but the redrive policy itself is
not applied by the app.

```hcl
resource "aws_sqs_queue" "orders_events_dlq" {
  name = "orders-events-dlq"
}
```

### SNS

```hcl
resource "aws_sns_topic" "orders_status" {
  name = "orders-status"
}
```

### SNS subscription

`topic_arn` and `endpoint` reference other resources in the same folder. The subscription
is applied only when the endpoint resource is among the ones being created.

```hcl
resource "aws_sns_topic_subscription" "orders_status_to_events" {
  topic_arn            = aws_sns_topic.orders_status.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.orders_events.arn
  raw_message_delivery = true
}
```

`filter_policy` works through `jsonencode`, one attribute per line, each value valid JSON
on its own line. Nested objects are not parsed.

```hcl
  filter_policy = jsonencode({
    eventType = ["created", "updated"]
  })
```

### S3

The parser looks for `name`, which this block does not have, so the bucket name comes from
the label with `_` replaced by `-`. Keep the label and `bucket` aligned or the app creates
a differently named bucket.

```hcl
resource "aws_s3_bucket" "orders_invoices" {
  bucket = "orders-invoices"
}
```

### DynamoDB

The table is always created with a single `id` partition key of type `S` and
`PAY_PER_REQUEST`, whatever the file declares. Do not design around other keys.

```hcl
resource "aws_dynamodb_table" "orders_table" {
  name         = "orders-table"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}
```

### Step Functions

Only `name` is used. The emulator receives a single pass-through state, so the definition
below is documentation, not behaviour.

```hcl
resource "aws_sfn_state_machine" "orders_flow" {
  name     = "orders-flow"
  role_arn = "arn:aws:iam::000000000000:role/stepfunctions-role"

  definition = jsonencode({
    Comment = "orders-flow",
    StartAt = "HelloWorld",
    States  = {
      HelloWorld = { Type = "Pass", End = true }
    }
  })
}
```

### ElastiCache

The name comes from `cluster_id`. With `engine = "redis"` a replication group is created,
with `engine = "memcached"` a cache cluster. Numeric attributes are only read when quoted,
and `port` is never sent: Redis answers on 6379, Memcached on 11211.

```hcl
resource "aws_elasticache_cluster" "orders_cache" {
  cluster_id      = "orders-cache"
  engine          = "redis"
  node_type       = "cache.t3.micro"
  num_cache_nodes = 1
}
```

### SSM Parameter Store

`name` is required here and is the only source of the parameter name: a block label cannot
contain a slash, so a block without `name` is skipped. The app sends `name`, `value` and
`type` with `put-parameter --overwrite`, so creating an existing parameter replaces its
value.

```hcl
resource "aws_ssm_parameter" "orders_db_host" {
  name  = "/orders/db/host"
  type  = "String"
  value = "localhost"
}
```

A `SecureString` value written into a `.tf` file is a secret in plain text in the user's
repository, and the Inspector shows it decrypted. Keep local debugging on `String` and
never move a real secret into these files.

## Step 5: payloads.json (optional)

Saved payloads for the Running screen. Every entry requires `name`, `queue` and `payload`.
`queue` is the only targeting field: there is no `topic` key. To target an SNS topic, put
the topic name, or the name of a queue subscribed to it, in `queue`.

A single malformed entry makes the whole file load as empty, silently. Keep the shape exact.

```json
[
  {
    "name": "Order created",
    "queue": "orders-events",
    "payload": {
      "orderId": "abc-123",
      "status": "created"
    }
  },
  {
    "name": "Status changed",
    "queue": "orders-status",
    "payload": {
      "orderId": "abc-123",
      "status": "shipped"
    }
  }
]
```

## Step 6: verify before reporting done

Run these checks and fix anything they catch.

```bash
test -f infra/aws-local.config.json && python3 -m json.tool infra/aws-local.config.json >/dev/null
ls infra/*.tf >/dev/null 2>&1 && grep -h -c 'resource "aws_' infra/*.tf
grep -h -o 'resource "aws_[a-z0-9_]*" "[^"]*"' infra/*.tf
test -f infra/payloads.json && python3 -m json.tool infra/payloads.json >/dev/null
```

Confirm each item:

- At least one `resource "aws_..."` block exists, otherwise the project is invisible.
- No block label contains `-` or `.`.
- Every `aws_s3_bucket` label matches its `bucket` value, with `_` in place of `-`.
- Every `aws_ssm_parameter` block has a `name`, otherwise the app skips it.
- Every `.tf` file is directly in `infra/`, none in a subfolder.
- Every `payloads.json` entry has `name`, `queue` and `payload`.
- Resource names match the names the application code actually uses.

Finish by telling the user the parent directory to configure in Settings → Projects
Directory, and that the resources are created from the Infrastructure screen.
