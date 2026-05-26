package dev.lucascosta.awslocalmanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dev.lucascosta.awslocalmanager.components.SideNav
import dev.lucascosta.awslocalmanager.components.TopBar
import dev.lucascosta.awslocalmanager.components.UpdateDialog
import dev.lucascosta.awslocalmanager.constants.AppConstants.APP_NAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.WINDOW_HEIGHT_DP
import dev.lucascosta.awslocalmanager.constants.AppConstants.WINDOW_WIDTH_DP
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.resources.*
import dev.lucascosta.awslocalmanager.data.remote.EmulatorClient
import dev.lucascosta.awslocalmanager.data.repository.AppPreferences
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.di.appModules
import dev.lucascosta.awslocalmanager.features.dashboard.DashboardViewModel
import dev.lucascosta.awslocalmanager.features.infrastructure.InfrastructureViewModel
import dev.lucascosta.awslocalmanager.features.inspector.InspectorViewModel
import dev.lucascosta.awslocalmanager.features.inspector.handler.*
import dev.lucascosta.awslocalmanager.features.project.ProjectSelectorViewModel
import dev.lucascosta.awslocalmanager.features.quick.QuickViewModel
import dev.lucascosta.awslocalmanager.features.running.RunningViewModel
import dev.lucascosta.awslocalmanager.features.settings.SettingsViewModel
import dev.lucascosta.awslocalmanager.features.setup.SetupViewModel
import dev.lucascosta.awslocalmanager.features.update.UpdateViewModel
import dev.lucascosta.awslocalmanager.i18n.LocalInspectorStrings
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.i18n.inspectorStringsForLanguage
import dev.lucascosta.awslocalmanager.i18n.stringsForLanguage
import dev.lucascosta.awslocalmanager.navigation.AppNavigation
import dev.lucascosta.awslocalmanager.navigation.Screen
import dev.lucascosta.awslocalmanager.theme.AppTheme
import dev.lucascosta.awslocalmanager.theme.DesktopAppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import java.awt.Desktop
import java.net.URI

fun main() {
    ResourceRegistry.register(
        SnsResource,
        S3Resource,
        DynamoDbResource,
        SqsResource,
        StepFunctionsResource,
        SnsSubscriptionResource,
        ElastiCacheResource,
    )

    InspectorHandlerRegistry.register(SqsInspectorHandler())
    InspectorHandlerRegistry.register(StepFunctionsInspectorHandler())
    InspectorHandlerRegistry.register(DynamoInspectorHandler())
    InspectorHandlerRegistry.register(S3InspectorHandler())
    InspectorHandlerRegistry.register(ElastiCacheInspectorHandler())

    application {
        val windowState = WindowState(size = DpSize(WINDOW_WIDTH_DP.dp, WINDOW_HEIGHT_DP.dp))

        Window(
            onCloseRequest = ::exitApplication,
            title = APP_NAME,
            state = windowState,
        ) {
            KoinApplication(application = { modules(appModules) }) {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val emulatorClient: EmulatorClient = koinInject()
    val preferencesRepository: PreferencesRepository = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()
    val updateViewModel: UpdateViewModel = koinInject()

    val allViewModels =
        listOf(
            koinInject<DashboardViewModel>(),
            settingsViewModel,
            koinInject<SetupViewModel>(),
            koinInject<ProjectSelectorViewModel>(),
            koinInject<InfrastructureViewModel>(),
            koinInject<RunningViewModel>(),
            koinInject<QuickViewModel>(),
            koinInject<InspectorViewModel>(),
            updateViewModel,
        )

    DisposableEffect(Unit) {
        onDispose {
            emulatorClient.close()
            allViewModels.forEach { it.onCleared() }
        }
    }

    val prefs by preferencesRepository.preferences.collectAsState(initial = AppPreferences())
    val updateAvailable by updateViewModel.updateAvailable.collectAsState()

    CompositionLocalProvider(
        LocalStrings provides stringsForLanguage(prefs.language),
        LocalInspectorStrings provides inspectorStringsForLanguage(prefs.language),
    ) {
        DesktopAppTheme(appTheme = prefs.theme) {
            AppContent(
                currentTheme = prefs.theme,
                currentLanguage = prefs.language,
                autoCheckEnv = prefs.autoCheckEnv,
                onThemeToggle = { newTheme -> settingsViewModel.setTheme(newTheme) },
                onLanguageSelected = { lang -> settingsViewModel.setLanguage(lang) },
            )

            updateAvailable?.let { release ->
                UpdateDialog(
                    release = release,
                    onDownload = {
                        runCatching { Desktop.getDesktop().browse(URI(release.releaseUrl)) }
                        updateViewModel.dismissUpdate()
                    },
                    onSkip = { updateViewModel.skipVersion(release.version) },
                    onDismiss = { updateViewModel.dismissUpdate() },
                )
            }
        }
    }
}

@Composable
fun AppContent(
    currentTheme: AppTheme,
    currentLanguage: String,
    autoCheckEnv: Boolean,
    onThemeToggle: (AppTheme) -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    val dashboardViewModel: DashboardViewModel = koinInject()
    val setupViewModel: SetupViewModel = koinInject()

    val dashboardState by dashboardViewModel.state.collectAsState()

    val initialScreen: Screen =
        remember(autoCheckEnv) {
            if (autoCheckEnv) Screen.Setup else Screen.Dashboard
        }

    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    var isSideNavExpanded by remember { mutableStateOf(false) }

    var prevScreen by remember { mutableStateOf<Screen?>(null) }
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.Setup && prevScreen != null) {
            setupViewModel.runChecks()
        }
        prevScreen = currentScreen
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                isConnected = dashboardState.isConnected,
                currentTheme = currentTheme,
                currentLanguage = currentLanguage,
                onThemeToggle = onThemeToggle,
                onLanguageSelected = onLanguageSelected,
            )

            Row(modifier = Modifier.fillMaxSize()) {
                SideNav(
                    currentScreen = currentScreen,
                    isExpanded = isSideNavExpanded,
                    onScreenSelected = { currentScreen = it },
                    onToggleExpand = { isSideNavExpanded = !isSideNavExpanded },
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it },
                    )
                }
            }
        }
    }
}
