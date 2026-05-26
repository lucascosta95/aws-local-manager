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
- ⚡ **Quick Create** — spin up SQS queues, SNS topics, S3 buckets, and DynamoDB tables without Terraform
- 📤 **Message publishing** — send JSON messages to SQS, SNS, DynamoDB, and Step Functions; upload files to S3
- 🔁 **Step Functions execution** — trigger state machine executions with custom JSON input
- 💾 **Saved payloads** — store and reuse common message payloads per project via `payloads.json`
- 🌍 **i18n** — interface available in English and Portuguese (pt-BR)
- 🎨 **Light and dark theme**
- 🔄 **Auto-update** via GitHub Releases

**Supported services:** SQS · SNS · S3 · DynamoDB · Step Functions

---

## 📋 Prerequisites

| Dependency | Purpose |
|---|---|
| JDK 17+ | Runtime |
| Docker | Runs the AWS emulator container |
| AWS CLI | Used internally to provision resources |

Pull the emulator image before first launch:

```bash
docker pull floci/floci:1.5.19
```

> The Setup screen checks all prerequisites on launch and can auto-fix most issues.

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
│       ├── aws-local.config.json  ← project metadata (name, description)
│       ├── queues.tf
│       ├── topics.tf
│       └── payloads.json          ← saved message payloads (optional)
└── another-service/
    └── infra/
        ├── aws-local.config.json
        └── tables.tf
```

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

Output is placed in `desktop/build/compose/binaries/`.

---

## 📄 License

[MIT](LICENSE)
