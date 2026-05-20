package dev.lucascosta.awslocalmanager.di

import dev.lucascosta.awslocalmanager.features.dashboard.DashboardViewModel
import dev.lucascosta.awslocalmanager.features.infrastructure.InfrastructureViewModel
import dev.lucascosta.awslocalmanager.features.inspector.InspectorViewModel
import dev.lucascosta.awslocalmanager.features.project.ProjectSelectorViewModel
import dev.lucascosta.awslocalmanager.features.quick.QuickViewModel
import dev.lucascosta.awslocalmanager.features.running.RunningViewModel
import dev.lucascosta.awslocalmanager.features.settings.SettingsViewModel
import dev.lucascosta.awslocalmanager.features.setup.SetupViewModel
import dev.lucascosta.awslocalmanager.features.update.UpdateViewModel
import org.koin.dsl.module

val presentationModule =
    module {
        single { DashboardViewModel(get(), get(), get()) }
        single { SettingsViewModel(get()) }
        single { SetupViewModel(get(), get()) }
        single { ProjectSelectorViewModel(get(), get(), get(), get()) }
        single { InfrastructureViewModel(get(), get(), get(), get()) }
        single {
            RunningViewModel(
                preferencesRepository = get(),
                terraformReader = get(),
                serviceHealthRepository = get(),
                savedPayloadRepository = get(),
                runningResourceRepository = get(),
                messageRepositoryFactory = get(),
                associateResources = get(),
            )
        }
        single { QuickViewModel(get()) }
        single { UpdateViewModel(get(), get()) }
        single { InspectorViewModel(get()) }
    }
