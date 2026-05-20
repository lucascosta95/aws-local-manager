package dev.lucascosta.awslocalmanager.data.model.navigation

import androidx.compose.runtime.Composable

data class NavGroup(
    val titleFn: @Composable () -> String,
    val items: List<NavItem>,
)
