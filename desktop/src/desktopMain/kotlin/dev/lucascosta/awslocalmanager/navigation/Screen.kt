package dev.lucascosta.awslocalmanager.navigation

import dev.lucascosta.awslocalmanager.data.model.project.InfraProject

sealed class Screen {
    object Dashboard : Screen()

    object Running : Screen()

    object Project : Screen()

    data class Infrastructure(val project: InfraProject) : Screen()

    object Quick : Screen()

    object Inspector : Screen()

    object Settings : Screen()

    object Setup : Screen()
}
