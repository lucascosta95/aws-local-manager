🇺🇸 [English version](README.md)

# AWS Local Manager

![License](https://img.shields.io/github/license/lucascosta95/aws-local-manager)
![Release](https://img.shields.io/github/v/release/lucascosta95/aws-local-manager)
![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Linux-lightgrey)
![Built with Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)

Interface gráfica desktop para gerenciar serviços AWS emulados localmente — feita para desenvolvedores backend que trabalham com AWS em ambiente de desenvolvimento.

---

## 🧩 O Problema

Trabalhar com um emulador local de AWS (como o [Floci](https://hub.docker.com/r/hectorvent/floci)) significa executar comandos AWS CLI ou Terraform manualmente para cada operação: criar filas, publicar mensagens, verificar a saúde dos serviços, destruir recursos. É lento, propenso a erros e quebra o fluxo de desenvolvimento.

## 💡 Solução

O AWS Local Manager oferece uma interface visual integrada aos seus projetos Terraform e ao emulador Floci. Crie e destrua recursos a partir dos seus arquivos `.tf` existentes, publique mensagens, monitore a saúde dos serviços — tudo em uma única janela.

---

## ✨ Funcionalidades

- 🩺 **Dashboard de saúde em tempo real** — monitore todos os serviços AWS emulados com intervalo de polling configurável
- 🏗️ **Infraestrutura via Terraform** — leia seus arquivos `.tf` e provisione recursos diretamente no emulador sem precisar rodar `terraform apply`
- ⚡ **Criação rápida** — crie filas SQS, tópicos SNS, buckets S3 e tabelas DynamoDB sem Terraform
- 📤 **Publicação de mensagens** — envie mensagens JSON para SQS, SNS, DynamoDB e Step Functions; faça upload de arquivos para o S3
- 🔁 **Execução de Step Functions** — dispare execuções de máquinas de estado com input JSON personalizado
- 💾 **Payloads salvos** — armazene e reutilize mensagens comuns por projeto via `payloads.json`
- 🌍 **i18n** — interface disponível em inglês e português (pt-BR)
- 🎨 **Tema claro e escuro**
- 🔍 **Inspector** — navegue e inspecione o conteúdo de filas SQS, execuções de Step Functions, tabelas DynamoDB, buckets S3 e chaves ElastiCache diretamente pelo app
- 🔄 **Auto-update** via GitHub Releases

**Serviços suportados:** SQS · SNS · S3 · DynamoDB · Step Functions · ElastiCache

---

## 📋 Pré-requisitos

| Dependência | Finalidade |
|---|---|
| JDK 17+ | Runtime |
| Docker | Executa o container do emulador AWS |
| AWS CLI | Usado internamente para provisionar recursos |

Baixe a imagem do emulador antes de usar pela primeira vez:

```bash
docker pull floci/floci:1.5.19
```

> A tela de Setup verifica todos os pré-requisitos na inicialização e pode corrigir a maioria dos problemas com um clique.

---

## 📦 Instalação

### macOS / Linux — via script (sem instalação formal)

Ideal para ambientes onde não é possível instalar aplicações.
Requer apenas **Java 17+** e **curl**.

**1. Instale o script uma única vez:**
```bash
curl -fsSL https://raw.githubusercontent.com/lucascosta95/aws-local-manager/main/scripts/awslocal \
  -o ~/.local/bin/awslocal && chmod +x ~/.local/bin/awslocal
```

**2. Use a partir de qualquer terminal:**
```bash
awslocal
```

O script verifica automaticamente se há uma nova versão disponível a cada execução e atualiza o cache em `~/.aws-local-manager/` quando necessário.

---

### macOS — instalador nativo (.dmg)

Baixe o `.dmg` na [página de releases](https://github.com/lucascosta95/aws-local-manager/releases/latest) e arraste para Applications.

### Linux — instalador nativo (.deb)

Baixe o `.deb` na [página de releases](https://github.com/lucascosta95/aws-local-manager/releases/latest) e execute:

```bash
sudo dpkg -i aws-local-manager_*.deb
```

---

## 🚀 Como usar

1. **Setup** — na primeira execução, o app verifica Docker, o container do emulador e o AWS CLI. Corrija qualquer problema com um clique.
2. **Dashboard** — confirme que todos os serviços estão saudáveis antes de começar.
3. **Infraestrutura** — aponte o app para o diretório dos seus projetos, selecione um projeto e aplique os recursos Terraform no emulador.
4. **Em Execução** — navegue pelos recursos ativos, publique mensagens e exclua recursos quando terminar.

---

## 📁 Estrutura de Projetos

O AWS Local Manager descobre projetos escaneando um diretório que você configura em **Configurações → Diretório de Projetos**. Cada projeto deve seguir esta estrutura:

```
~/projetos/                            ← diretório raiz configurado
├── minha-api/
│   └── infra/
│       ├── aws-local.config.json      ← metadados do projeto (nome, descrição)
│       ├── filas.tf
│       ├── topicos.tf
│       └── payloads.json              ← payloads salvos (opcional)
└── outro-servico/
    └── infra/
        ├── aws-local.config.json
        └── tabelas.tf
```

### payloads.json

Armazene payloads de mensagens junto com os arquivos de infra e reutilize-os na tela de Recursos em Execução:

```json
[
  {
    "name": "Pedido criado",
    "queue": "fila-pedidos",
    "payload": {
      "pedidoId": "abc-123",
      "clienteId": "usr-456",
      "status": "criado",
      "itens": [
        { "sku": "PROD-01", "qtd": 2 }
      ]
    }
  },
  {
    "name": "Pagamento aprovado",
    "topic": "topico-pagamentos",
    "payload": {
      "pedidoId": "abc-123",
      "valor": 99.90,
      "moeda": "BRL"
    }
  }
]
```

---

## 🛠️ Desenvolvimento

```bash
git clone https://github.com/lucascosta95/aws-local-manager.git
cd aws-local-manager
./gradlew :desktop:run
```

Gerar pacotes nativos:

```bash
# Linux (.deb)
./gradlew :desktop:packageDeb

# macOS (.dmg)
./gradlew :desktop:packageDmg
```

Os arquivos gerados ficam em `desktop/build/compose/binaries/`.

---

## 📄 Licença

[MIT](LICENSE)
