package dev.lucascosta.awslocalmanager.di

import dev.lucascosta.awslocalmanager.domain.AssociateResourcesUseCase
import dev.lucascosta.awslocalmanager.domain.AwsResourceChecker
import dev.lucascosta.awslocalmanager.domain.DynamoDbHealthProbe
import dev.lucascosta.awslocalmanager.domain.S3HealthProbe
import dev.lucascosta.awslocalmanager.domain.ServiceStatusChecker
import dev.lucascosta.awslocalmanager.domain.SnsHealthProbe
import dev.lucascosta.awslocalmanager.domain.SqsHealthProbe
import dev.lucascosta.awslocalmanager.domain.StepFunctionsHealthProbe
import dev.lucascosta.awslocalmanager.domain.TerraformReader
import org.koin.dsl.module

val domainModule =
    module {
        single { TerraformReader() }

        single {
            ServiceStatusChecker(
                probes =
                    listOf(
                        SqsHealthProbe(),
                        SnsHealthProbe(),
                        S3HealthProbe(),
                        DynamoDbHealthProbe(),
                        StepFunctionsHealthProbe(),
                    ),
            )
        }

        single { AwsResourceChecker(get()) }
        single { AssociateResourcesUseCase() }
    }
