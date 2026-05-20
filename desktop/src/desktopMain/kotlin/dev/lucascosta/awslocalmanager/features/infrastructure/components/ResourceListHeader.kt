package dev.lucascosta.awslocalmanager.features.infrastructure.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun ResourceListHeader(
    totalCount: Int,
    selectedCount: Int,
    typeFilter: AwsResourceDefinition?,
    availableTypes: List<AwsResourceDefinition>,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onFilterChange: (AwsResourceDefinition?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                strings.infraResourceCountFmt
                    .replace("{total}", totalCount.toString())
                    .replace("{selected}", selectedCount.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
            )

            TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(strings.infraSelectAll, style = MaterialTheme.typography.labelSmall)
            }

            TextButton(onClick = onDeselectAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(strings.infraDeselectAll, style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text(strings.infraFilterAll, style = MaterialTheme.typography.labelSmall) },
            )

            availableTypes.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { onFilterChange(if (typeFilter == type) null else type) },
                    label = { Text(type.id, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
