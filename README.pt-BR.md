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
- ⚡ **Criação rápida** — crie filas SQS, tópicos SNS, buckets S3, tabelas DynamoDB, parâmetros SSM, clusters e tópicos MSK, e registries e schemas do Glue sem Terraform
- 📤 **Publicação de mensagens** — envie mensagens JSON para SQS, SNS, DynamoDB, Step Functions e tópicos Kafka (MSK); faça upload de arquivos para o S3
- 🔁 **Execução de Step Functions** — dispare execuções de máquinas de estado com input JSON personalizado
- 💾 **Payloads salvos** — armazene e reutilize mensagens comuns por projeto via `payloads.json`
- 🌍 **i18n** — interface disponível em inglês e português (pt-BR)
- 🎨 **Tema claro e escuro**
- 🔍 **Inspector** — navegue e inspecione o conteúdo de filas SQS, execuções de Step Functions, tabelas DynamoDB, buckets S3, chaves ElastiCache, parâmetros SSM, tópicos, mensagens, consumer groups e schemas do MSK, e schemas e versões do Glue Schema Registry diretamente pelo app
- 🔄 **Auto-update** via GitHub Releases

**Serviços suportados:** SQS · SNS · S3 · DynamoDB · Step Functions · ElastiCache · SSM Parameter Store · MSK (Kafka) · Glue Schema Registry

---

## 📋 Pré-requisitos

| Dependência | Finalidade |
|---|---|
| JDK 17+ | Runtime |
| Docker | Executa o container do emulador AWS |
| AWS CLI | Usado internamente para provisionar recursos |

Baixe a imagem do emulador antes de usar pela primeira vez:

```bash
docker pull floci/floci:2.0.1
```

> A tela de Setup verifica todos os pré-requisitos na inicialização e pode corrigir a maioria dos problemas com um clique.

O app procura `docker`, `colima` e `aws` no `PATH` e, se não achar, nos lugares onde essas
ferramentas costumam ser instaladas (`/opt/homebrew/bin`, `/usr/local/bin`, `~/.docker/bin`,
`~/.rd/bin`, entre outros). Isso importa no macOS: um app aberto pelo Finder é iniciado pelo launchd
com um `PATH` de `/usr/bin:/bin:/usr/sbin:/sbin` e nunca lê o perfil do seu shell, então uma
instalação via Homebrew fica invisível para ele mesmo que as mesmas ferramentas funcionem no
terminal. A tela de Logs registra onde cada ferramenta foi encontrada, ou que não foi.

Cada release é fixada em uma versão do emulador. Ao atualizar de uma release que usava outra, o Setup
mostra a imagem e o emulador como **Desatualizado**: o Docker mantém a imagem antiga e continua
servindo ela ao container já criado a partir dela, então um `docker pull` sozinho não resolve.
Corrigir a verificação da imagem baixa a versão suportada, remove o container que o app criou com a
imagem antiga, e apaga a imagem antiga. Corrigir a verificação do emulador recria o container na
versão suportada. Tudo que for removido aparece no log da correção.

O container do emulador também precisa rodar na rede Docker `aws-local-manager`, com
`FLOCI_SERVICES_DOCKER_NETWORK` e `FLOCI_SERVICES_MSK_DEFAULT_IMAGE` definidas, para que os brokers Kafka
que ele sobe consigam resolver o próprio nome. Um container criado por uma release anterior não tem
isso, e o Setup também mostra o emulador como **Desatualizado** por esse motivo. A correção cria a rede
quando ela não existe e recria o container. O emulador não guarda estado, então recriá-lo apaga todos os
recursos criados até ali.

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
│       ├── aws-local.config.json      ← metadados do projeto (nome)
│       ├── filas.tf
│       ├── topicos.tf
│       └── payloads.json              ← payloads salvos (opcional)
└── outro-servico/
    └── infra/
        ├── aws-local.config.json
        └── tabelas.tf
```

### aws-local.config.json

Identifica o projeto dentro do app. Apenas `name` é obrigatório:

```json
{
  "name": "Orders API"
}
```

### Logs da sessão

A entrada **Logs** na barra lateral mostra tudo que o app fez desde que abriu: cada comando externo
executado com o código de saída, cada falha ao consultar a saúde do emulador, e cada exceção
capturada, com o stack trace a um clique. É o único lugar onde essas exceções aparecem — no resto do
app ele informa que algo falhou sem dizer o que foi lançado.

O buffer vive em memória, é limitado a 2000 entradas e nunca é gravado em disco, então fechar o app
descarta tudo. Entradas idênticas em sequência viram uma só com um contador, o que evita que uma
falha em polling soterre o resto. Dá para filtrar por nível, por origem ou por texto livre na
mensagem e no stack trace, copiar o que está visível, ou limpar.

A lista só acompanha a entrada mais nova enquanto você está no fim dela: subir o scroll interrompe,
voltar ao fim retoma, e o chip Tail pula para o final. Passar o mouse numa linha revela um botão que
copia aquela entrada junto com o stack trace.

Os painéis de log que já existem nas telas de Infraestrutura e Setup continuam iguais; esta é uma
visão separada sobre tudo de uma vez.

### Skill para agentes de IA

A tela **Skills** instala a skill do repositório nas ferramentas de IA encontradas na sua pasta
pessoal. Depois você abre qualquer projeto na ferramenta que preferir, chama a skill, e ela varre
aquele projeto atrás dos serviços AWS que o código já usa — lendo o fonte, as configurações e os
arquivos `.env` — e escreve o `infra/` com a estrutura correta.

Todas as ferramentas recebem o mesmo formato Agent Skills, uma pasta por skill, então as instruções
são carregadas só quando você chama a skill e nunca ficam em conversas que não têm a ver.

| Ferramenta | Onde é gravada | Como chamar |
|---|---|---|
| Claude Code | `~/.claude/skills/aws-local-infra/SKILL.md` | `/aws-local-infra` |
| Cursor | `~/.cursor/skills/aws-local-infra/SKILL.md` | `/aws-local-infra` |
| Codex CLI | `~/.codex/skills/aws-local-infra/SKILL.md` | `$aws-local-infra` |
| Gemini CLI | `~/.gemini/skills/aws-local-infra/SKILL.md` | escolhida pela descrição; `/skills` lista |

A tela mostra todos os caminhos absolutos antes de gravar, e o catálogo vem dentro do app e é
atualizado pelo GitHub quando há conexão. Reinicie o agente depois de instalar, porque as quatro
ferramentas leem a pasta de skills na inicialização.

As versões até a 1.2.0 gravavam uma rule do Cursor em `~/.cursor/rules` e acrescentavam um bloco ao
`~/.codex/AGENTS.md` e ao `~/.gemini/GEMINI.md`. Instalar de novo remove os dois, guardando uma
cópia `.bak` dos arquivos de instrução.

Para instalar na mão, ou em um único projeto em vez de globalmente, troque `.claude` por `.cursor`,
`.codex` ou `.gemini`:

```bash
mkdir -p .claude/skills/aws-local-infra && \
curl -fsSL https://raw.githubusercontent.com/lucascosta95/aws-local-manager/main/skills/aws-local-infra/SKILL.md \
  -o .claude/skills/aws-local-infra/SKILL.md
```

A skill é Markdown puro com frontmatter `name` e `description`, então também funciona colada no
`AGENTS.md` ou no prompt de qualquer outro assistente.

A skill em si é escrita em inglês, porque é o que o agente lê. A tela em volta dela segue o idioma
selecionado no app: cada entrada do `skills/catalog.json` tem um mapa `translations` com a tag de
idioma como chave, e uma skill publicada sem tradução cai no `name` e `description` em inglês.
O **Ver conteúdo** mostra sempre a skill como ela é instalada.

### Templates Terraform

O app nunca executa `terraform apply`. Ele lê os arquivos `.tf` com um parser simples e chama o AWS CLI apontando para o emulador, então os arquivos podem ser bem enxutos — não é preciso `provider`, `backend`, IAM, variáveis ou módulos.

Como o parser lê um arquivo:

- Apenas arquivos `.tf` **diretamente** dentro de `infra/` são lidos; subdiretórios são ignorados.
- Todo recurso precisa ser um bloco de primeiro nível: `resource "<tipo_aws>" "<label>" { ... }`. O label aceita apenas letras, números e `_`.
- O nome do recurso na AWS vem do atributo `name` do bloco. Quando ele não existe, o app usa o label com `_` trocado por `-`. As exceções são `aws_ssm_parameter` e `aws_msk_topic`, ignorados quando o `name` está ausente, e `aws_msk_cluster`, que lê `cluster_name`.
- Os valores precisam ser strings literais. `var.*`, `local.*` e interpolações `${...}` **não** são resolvidos.
- Qualquer outro atributo é ignorado pelo app e pode continuar no arquivo, então o mesmo `.tf` segue válido para um Terraform de verdade.

> 💡 Na tela de **Infraestrutura**, o botão **Criar template** gera um arquivo pronto para editar em qualquer um dos tipos abaixo.

#### SQS

```hcl
resource "aws_sqs_queue" "orders_events" {
  name = "orders-events"
}
```

Atributos de tempo e retry são aceitos e ficam no arquivo para execuções reais do Terraform, mas o app cria a fila com os valores padrão do emulador:

```hcl
resource "aws_sqs_queue" "orders_events" {
  name                       = "orders-events"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600
  delay_seconds              = 0
  receive_wait_time_seconds  = 0
}
```

Uma dead-letter queue é apenas uma segunda fila. O app cria as duas, mas a política de redrive em si não é aplicada no emulador — use a **Criação rápida** quando precisar da fila já ligada a uma DLQ:

```hcl
resource "aws_sqs_queue" "orders_events_dlq" {
  name = "orders-events-dlq"
}

resource "aws_sqs_queue" "orders_events" {
  name = "orders-events"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.orders_events_dlq.arn
    maxReceiveCount     = 3
  })
}
```

#### SNS

```hcl
resource "aws_sns_topic" "orders_status" {
  name = "orders-status"
}
```

#### Inscrição SNS

`topic_arn` e `endpoint` podem referenciar outro recurso declarado na mesma pasta (`aws_sns_topic.<label>.arn`, `aws_sqs_queue.<label>.arn`) ou conter um ARN literal. A inscrição só é aplicada quando o recurso de endpoint está entre os recursos selecionados:

```hcl
resource "aws_sns_topic_subscription" "orders_status_to_events" {
  topic_arn            = aws_sns_topic.orders_status.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.orders_events.arn
  raw_message_delivery = true
}
```

`filter_policy` é suportado via `jsonencode`, com um atributo por linha e um valor JSON válido em cada uma delas (objetos aninhados não são interpretados):

```hcl
resource "aws_sns_topic_subscription" "orders_status_to_events" {
  topic_arn = aws_sns_topic.orders_status.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.orders_events.arn

  filter_policy       = jsonencode({
    eventType = ["created", "updated"]
    priority  = ["high"]
  })
  filter_policy_scope = "MessageAttributes"
}
```

#### S3

O parser procura por `name`, atributo que um bloco `aws_s3_bucket` não tem, então o nome do bucket vem do label com `_` trocado por `-`. Mantenha o label e o valor de `bucket` alinhados:

```hcl
resource "aws_s3_bucket" "orders_invoices" {
  bucket = "orders-invoices"
}
```

#### DynamoDB

A tabela é sempre criada com uma única chave de partição `id` do tipo `S` e cobrança `PAY_PER_REQUEST`, independentemente das chaves declaradas no arquivo:

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

#### Step Functions

Apenas `name` é usado. A máquina de estado é criada no emulador com um único estado de passagem, então a `definition` abaixo serve para execuções reais do Terraform e como documentação:

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

#### ElastiCache

O nome vem de `cluster_id`. Com `engine = "redis"` o app cria um replication group; com `engine = "memcached"` ele cria um cache cluster usando `num_cache_nodes`:

```hcl
resource "aws_elasticache_cluster" "orders_cache" {
  cluster_id      = "orders-cache"
  engine          = "redis"
  node_type       = "cache.t3.micro"
  num_cache_nodes = 1
  port            = 6379
}
```

O parser só lê valores entre aspas, então um `num_cache_nodes = 1` sem aspas cai no padrão de um único nó — escreva `num_cache_nodes = "2"` quando um cluster Memcached precisar de mais. A `port` nunca é enviada ao emulador: o Redis responde em `6379` e o Memcached em `11211`.

#### SSM Parameter Store

O `name` é obrigatório neste tipo. Um label de bloco Terraform não aceita barra, então o fallback normal para o label inventaria um nome de parâmetro errado — um bloco sem `name` é ignorado pelo app.

```hcl
resource "aws_ssm_parameter" "orders_db_host" {
  name  = "/orders/db/host"
  type  = "String"
  value = "localhost"
}
```

O app envia `name`, `value` e `type` ao emulador com `put-parameter --overwrite`, então criar um parâmetro que já existe substitui o valor e incrementa a versão. Um `value` ausente é enviado como string vazia e um `type` ausente cai no padrão `String`.

> ⚠️ Um valor `SecureString` escrito em um arquivo `.tf` é um segredo em texto puro no seu repositório, e o Inspector o exibe descriptografado. Para depuração local, prefira `String`.

#### Amazon MSK (Kafka)

O emulador sustenta cada cluster MSK com um container [Redpanda](https://redpanda.com), que fala o protocolo Kafka. Um tópico aponta para o cluster pelo `cluster_arn`, seja como referência a um `aws_msk_cluster` declarado em qualquer arquivo da pasta, seja como ARN literal:

```hcl
resource "aws_msk_cluster" "orders" {
  cluster_name           = "orders-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 1

  broker_node_group_info {
    instance_type  = "kafka.t3.small"
    client_subnets = ["subnet-local"]
  }
}

resource "aws_msk_topic" "order_created" {
  name               = "order-created"
  cluster_arn        = aws_msk_cluster.orders.arn
  partition_count    = 3
  replication_factor = 1
}
```

- O cluster é criado com `kafka_version`, `number_of_broker_nodes` e `instance_type`. Subnets, security groups, criptografia, autenticação, logs e monitoramento são ignorados, então referências como `aws_subnet.a.id` não atrapalham.
- Seja qual for o `number_of_broker_nodes`, o emulador roda **um** broker. Por isso os tópicos são criados com replication factor `1`, e `configs` não é aplicado.
- Um tópico cujo `cluster_arn` aponta para um cluster que não está na pasta é ignorado.
- O primeiro cluster baixa a imagem do broker (cerca de 125 MB), então pode levar um minuto. Os tópicos são criados assim que o cluster fica ativo.
- Os tópicos aparecem na tela de Recursos em Execução como `<cluster>/<tópico>`. Esse também é o nome para usar no `payloads.json`, onde só o nome do tópico também funciona.

**Conectando sua aplicação.** O broker se anuncia como `floci-msk-<id>:9092`, um nome que só resolve dentro da rede Docker `aws-local-manager`, então um cliente fora dessa rede não consegue segui-lo. O app contorna isso subindo um container de proxy (`grepplabs/kafka-proxy`) para cada cluster ativo, que reescreve esse endereço para uma porta em `localhost`. O Inspector mostra os dois endereços, com botão de copiar:

| Onde a aplicação roda | `bootstrap.servers` |
|---|---|
| Na sua máquina, por exemplo iniciada pela IDE | `localhost:19092` — os próximos clusters recebem `19093`, `19094`, … |
| Em um container na rede `aws-local-manager` | `floci-msk-<id>:9092` |

- O proxy sobe em até um intervalo de polling depois que o cluster fica ativo, e o primeiro baixa a imagem do proxy (cerca de 300 MB). Ele é removido quando o cluster é apagado ou o emulador para.
- As portas são distribuídas em ordem alfabética do nome do cluster, e um cluster recriado mantém a porta enquanto o app está aberto. Com um único cluster, o endereço é sempre `localhost:19092`.
- Os proxies continuam rodando depois que o app é fechado, então um serviço iniciado pela IDE segue conectado. Na próxima vez que o app abrir, ele remove os proxies cujo cluster não existe mais.
- As portas são publicadas apenas em `127.0.0.1`.

Para uma aplicação em container, entre na rede pelo Compose:

```yaml
services:
  orders-service:
    build: .
    environment:
      KAFKA_BOOTSTRAP_SERVERS: floci-msk-a1b2c3:9092
    networks:
      - aws-local-manager

networks:
  aws-local-manager:
    external: true
```

**Schema Registry (compatível com Confluent).** Cada cluster MSK também roda um schema registry que fala a API do Confluent Schema Registry, então `io.confluent:kafka-avro-serializer` e afins funcionam sem mudança. O app o expõe do mesmo jeito que o broker:

| Onde a aplicação roda | `schema.registry.url` |
|---|---|
| Na sua máquina, por exemplo iniciada pela IDE | `http://localhost:18081` — os próximos clusters recebem `18082`, `18083`, … |
| Em um container na rede `aws-local-manager` | `http://floci-msk-<id>:8081` |

Os schemas são registrados pela própria aplicação (`auto.register.schemas`) ou pela API REST do registry; não existe recurso Terraform para eles. O Inspector lista cada subject com a última versão, e copiar uma linha copia o schema. Se a sua aplicação usa o AWS Glue Schema Registry, veja a próxima seção.

> ⚠️ Nada sobrevive a um restart do emulador. Clusters, tópicos, schemas e mensagens precisam ser aplicados de novo.

#### Glue Schema Registry

Para aplicações que serializam mensagens Kafka com a SerDe do AWS Glue Schema Registry (`software.amazon.glue:schema-registry-serde`). Um schema aponta para o registry pelo `registry_arn`, seja como referência a um `aws_glue_registry` em qualquer arquivo da pasta, seja como ARN literal; sem `registry_arn` ele vai para o `default-registry`, como na AWS.

```hcl
resource "aws_glue_registry" "payments" {
  registry_name = "payments"
}

resource "aws_glue_schema" "payment_approved" {
  schema_name       = "payment-approved"
  registry_arn      = aws_glue_registry.payments.arn
  data_format       = "AVRO"
  compatibility     = "BACKWARD"
  schema_definition = file("${path.module}/schemas/payment-approved.avsc")
}
```

- `schema_name`, `data_format` (`AVRO`, `JSON` ou `PROTOBUF`) e `schema_definition` são obrigatórios; um bloco sem algum deles é ignorado. `compatibility` tem `BACKWARD` como padrão.
- `schema_definition` pode ser uma string entre aspas, um heredoc (`<<EOF` ou `<<-EOF`) ou `file("...")` com caminho relativo a `infra/`, com ou sem o prefixo `${path.module}/`. `jsonencode(...)` **não** é avaliado.
- Aplicar de novo é seguro: uma definição sem mudança mantém a versão, e uma definição editada vira uma nova versão, respeitando o modo de compatibilidade.
- O Inspector lista registries e schemas, e mostra todas as versões de um schema com a definição.

O registry fica atrás do endpoint da AWS, então a aplicação o acessa como acessa o SQS ou o S3 — sem proxy. Aponte a SerDe para o emulador no perfil local:

```properties
endpoint=http://localhost:4566
region=us-east-1
registry.name=payments
dataFormat=AVRO
schemaAutoRegistrationEnabled=false
```

junto com `AWS_ACCESS_KEY_ID=test` e `AWS_SECRET_ACCESS_KEY=test` no ambiente.

#### O que o app lê de cada tipo

| Tipo Terraform | Atributos usados | Criado no emulador como |
|---|---|---|
| `aws_sqs_queue` | `name` | Fila com os padrões do emulador |
| `aws_sns_topic` | `name` | Tópico |
| `aws_sns_topic_subscription` | `topic_arn`, `endpoint`, `protocol`, `raw_message_delivery`, `filter_policy`, `filter_policy_scope` | Inscrição |
| `aws_s3_bucket` | label do bloco | Bucket |
| `aws_dynamodb_table` | `name` | Tabela com chave de partição `id` (`S`), `PAY_PER_REQUEST` |
| `aws_sfn_state_machine` | `name` | Máquina de estado com um único `Pass` |
| `aws_elasticache_cluster` | `cluster_id`, `engine`, `node_type`, `num_cache_nodes` (entre aspas) | Replication group (redis) ou cache cluster (memcached) |
| `aws_ssm_parameter` | `name` (obrigatório), `value`, `type` | Parâmetro criado com `put-parameter --overwrite` |
| `aws_msk_cluster` | `cluster_name`, `kafka_version`, `number_of_broker_nodes`, `instance_type` | Cluster sustentado por um único broker Redpanda |
| `aws_msk_topic` | `name` (obrigatório), `cluster_arn` (obrigatório), `partition_count` | Tópico com replication factor `1` |
| `aws_glue_registry` | `registry_name` | Schema registry |
| `aws_glue_schema` | `schema_name`, `data_format`, `schema_definition` (todos obrigatórios), `registry_arn`, `compatibility` | Schema, ou uma nova versão dele quando a definição mudou |

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

Os arquivos gerados ficam em `desktop/build/compose/binaries/`. Cada pacote precisa ser gerado no
sistema de destino, porque o `jpackage` só produz o formato da máquina em que roda.

O bundle do macOS tira o ícone de `desktop/icons/icon.icns`, porque o `jpackage` só lê `.icns` ali e
ignora um `.png` sem avisar. Depois de trocar o `desktop/src/desktopMain/resources/icon.png`,
regenere num Mac:

```bash
./scripts/generate_icns.sh
```

A geração do `.dmg` falha se esse arquivo sumir ou não for um `.icns` de verdade, em vez de entregar
o ícone padrão do Java em silêncio. O Linux continua usando o `.png` direto, que o `.deb` aceita.

---

## 📄 Licença

[MIT](LICENSE)
