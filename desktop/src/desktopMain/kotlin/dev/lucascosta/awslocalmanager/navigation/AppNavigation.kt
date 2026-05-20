package dev.lucascosta.awslocalmanager.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import dev.lucascosta.awslocalmanager.features.dashboard.DashboardScreen
import dev.lucascosta.awslocalmanager.features.infrastructure.InfrastructureScreen
import dev.lucascosta.awslocalmanager.features.inspector.InspectorScreen
import dev.lucascosta.awslocalmanager.features.project.ProjectSelectorScreen
import dev.lucascosta.awslocalmanager.features.quick.QuickScreen
import dev.lucascosta.awslocalmanager.features.running.RunningScreen
import dev.lucascosta.awslocalmanager.features.settings.SettingsScreen
import dev.lucascosta.awslocalmanager.features.setup.SetupScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) +
                slideInHorizontally(
                    animationSpec = tween(200),
                    initialOffsetX = { it / 10 },
                ) togetherWith fadeOut(animationSpec = tween(150))
        },
        label = "screen_transition",
    ) { screen ->
        when (screen) {
            Screen.Dashboard -> DashboardScreen()
            Screen.Settings -> SettingsScreen()
            Screen.Setup ->
                SetupScreen(
                    onContinue = { onScreenSelected(Screen.Dashboard) },
                    onNavigateToDashboard = { onScreenSelected(Screen.Dashboard) },
                )

            Screen.Project ->
                ProjectSelectorScreen(
                    onProjectOpen = { project ->
                        onScreenSelected(Screen.Infrastructure(project))
                    },
                )

            is Screen.Infrastructure ->
                InfrastructureScreen(
                    project = screen.project,
                    onBack = { onScreenSelected(Screen.Project) },
                )
            Screen.Running -> RunningScreen()
            Screen.Quick -> QuickScreen()
            Screen.Inspector -> InspectorScreen()
        }
    }
}
