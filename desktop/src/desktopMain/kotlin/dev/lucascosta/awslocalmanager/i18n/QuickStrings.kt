package dev.lucascosta.awslocalmanager.i18n

import androidx.compose.runtime.compositionLocalOf
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH

val LocalQuickStrings = compositionLocalOf<QuickStrings> { QuickStringsPtBr }

fun quickStringsForLanguage(tag: String): QuickStrings =
    when (tag) {
        ENGLISH -> QuickStringsEnUs
        else -> QuickStringsPtBr
    }

data class QuickStrings(
    val quickMskCluster: String,
    val quickMskNoClusters: String,
    val quickMskPartitions: String,
    val quickGlueRegistry: String,
    val quickGlueNoRegistries: String,
    val quickGlueDataFormat: String,
    val quickGlueCompatibility: String,
    val quickGlueDefinition: String,
)

val QuickStringsEnUs =
    QuickStrings(
        quickMskCluster = "Cluster",
        quickMskNoClusters = "No MSK cluster running. Create a cluster first.",
        quickMskPartitions = "Partitions",
        quickGlueRegistry = "Registry",
        quickGlueNoRegistries = "No Glue registry found. Create a registry first.",
        quickGlueDataFormat = "Data format",
        quickGlueCompatibility = "Compatibility",
        quickGlueDefinition = "Schema definition",
    )

val QuickStringsPtBr =
    QuickStrings(
        quickMskCluster = "Cluster",
        quickMskNoClusters = "Nenhum cluster MSK em execução. Crie um cluster primeiro.",
        quickMskPartitions = "Partições",
        quickGlueRegistry = "Registry",
        quickGlueNoRegistries = "Nenhum registry do Glue encontrado. Crie um registry primeiro.",
        quickGlueDataFormat = "Formato",
        quickGlueCompatibility = "Compatibilidade",
        quickGlueDefinition = "Definição do schema",
    )
