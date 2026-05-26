package dev.lucascosta.awslocalmanager.di

import dev.lucascosta.awslocalmanager.domain.*
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
                        ElastiCacheHealthProbe(),
                    ),
            )
        }

        single { AwsResourceChecker(get()) }
        single { AssociateResourcesUseCase() }
    }
