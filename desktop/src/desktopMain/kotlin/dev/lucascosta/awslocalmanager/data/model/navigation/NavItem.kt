package dev.lucascosta.awslocalmanager.data.model.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.navigation.Screen

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelFn: @Composable () -> String,
)
