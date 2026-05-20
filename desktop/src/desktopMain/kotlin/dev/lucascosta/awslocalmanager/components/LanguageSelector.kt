package dev.lucascosta.awslocalmanager.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH
import dev.lucascosta.awslocalmanager.constants.AppConstants.PORTUGUESE
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
fun LanguageSelector(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    val languages =
        listOf(
            PORTUGUESE to strings.settingsLanguagePtBr,
            ENGLISH to strings.settingsLanguageEnUs,
        )

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = strings.topBarLanguage,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        },
    ) {
        languages.forEach { (tag, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onLanguageSelected(tag)
                    expanded = false
                },
                trailingIcon = {
                    if (currentLanguage == tag) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
    }
}
