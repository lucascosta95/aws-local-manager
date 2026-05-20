package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.JetBrainsMonoFontFamily

@Composable
fun JsonEditor(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    val strings = LocalStrings.current
    val resolvedPlaceholder = placeholder ?: strings.publisherJsonPlaceholder
    val borderColor =
        when {
            value.isBlank() -> MaterialTheme.colorScheme.outline
            !isValid -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(12.dp),
        textStyle =
            MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    resolvedPlaceholder,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            innerTextField()
        },
    )
}
