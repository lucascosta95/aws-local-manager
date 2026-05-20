package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons.AutoMirrored.Outlined
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.model.resources.StepFunctionsResource
import dev.lucascosta.awslocalmanager.features.running.RunningUiState
import dev.lucascosta.awslocalmanager.features.running.RunningViewModel
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
internal fun PublishPanel(
    state: RunningUiState,
    viewModel: RunningViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            strings.runningPublishTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()

        val target = state.publishTarget
        if (target == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Outlined.Send,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        strings.runningPublishNoTarget,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResourceTypeChip(target.type)
                Text(
                    strings.runningPublishTo.replace("{name}", target.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val definition = target.type
            val canSend =
                if (definition.hasFilePublish) {
                    state.s3FilePath != null
                } else {
                    state.publishJson.isNotBlank() && state.isJsonValid
                }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (definition.hasFilePublish) {
                    S3FilePanel(
                        selectedFilePath = state.s3FilePath,
                        s3Key = state.s3ObjectKey,
                        onFileSelected = viewModel::selectPublishFile,
                        onFileClear = viewModel::clearPublishFile,
                        onKeyChanged = viewModel::updateS3Key,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = viewModel::sendPublish,
                        enabled = canSend && !state.isSending,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(strings.publisherS3Uploading, style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text(strings.publisherS3Upload, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    val placeholder =
                        when (target.type) {
                            DynamoDbResource -> strings.publisherDynamoDbPlaceholder
                            StepFunctionsResource -> strings.publisherStepFunctionsPlaceholder
                            else -> strings.publisherJsonPlaceholder
                        }
                    JsonEditor(
                        value = state.publishJson,
                        onValueChange = viewModel::updatePublishJson,
                        isValid = state.isJsonValid,
                        placeholder = placeholder,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.isJsonValid && state.publishJson.isNotBlank()) {
                        Text(
                            strings.publisherJsonInvalid,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (state.publishJson.isNotBlank() && state.isJsonValid) {
                        Text(
                            strings.publisherJsonValid,
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.success,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (definition.supportsPayloads) {
                            OutlinedButton(
                                onClick = viewModel::openPayloadsDialog,
                                enabled = state.hasPayloadsFile,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(strings.runningPayloadsButton, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedButton(onClick = viewModel::formatPublishJson) {
                            Text(strings.publisherFormatJson, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = viewModel::sendPublish,
                            enabled = canSend && !state.isSending,
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.publisherSending, style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text(strings.publisherSend, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
