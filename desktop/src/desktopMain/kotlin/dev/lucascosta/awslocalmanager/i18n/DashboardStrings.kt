package dev.lucascosta.awslocalmanager.i18n

import androidx.compose.runtime.compositionLocalOf
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH

val LocalDashboardStrings = compositionLocalOf<DashboardStrings> { DashboardStringsPtBr }

fun dashboardStringsForLanguage(tag: String): DashboardStrings =
    when (tag) {
        ENGLISH -> DashboardStringsEnUs
        else -> DashboardStringsPtBr
    }

data class DashboardStrings(
    val serviceCaptions: Map<String, String>,
)

val DashboardStringsEnUs =
    DashboardStrings(
        serviceCaptions =
            mapOf(
                "dynamodb" to "Tables and items",
                "elasticache" to "Redis and Memcached",
                "glue" to "Schema Registry",
                "kafka" to "Kafka clusters and topics",
                "s3" to "Buckets and objects",
                "sns" to "Topics and subscriptions",
                "sqs" to "Queues and messages",
                "ssm" to "Parameter Store",
                "states" to "State machines",
            ),
    )

val DashboardStringsPtBr =
    DashboardStrings(
        serviceCaptions =
            mapOf(
                "dynamodb" to "Tabelas e itens",
                "elasticache" to "Redis e Memcached",
                "glue" to "Schema Registry",
                "kafka" to "Clusters e tópicos Kafka",
                "s3" to "Buckets e objetos",
                "sns" to "Tópicos e assinaturas",
                "sqs" to "Filas e mensagens",
                "ssm" to "Parameter Store",
                "states" to "Máquinas de estado",
            ),
    )
