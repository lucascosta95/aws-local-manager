package dev.lucascosta.awslocalmanager.di

import dev.lucascosta.awslocalmanager.data.remote.*
import dev.lucascosta.awslocalmanager.data.repository.*
import org.koin.dsl.module

val dataModule =
    module {
        single { PreferencesRepository() }
        single { EmulatorClient() }

        factory { (endpoint: String) -> AwsSnsClient(endpoint) }
        factory { (endpoint: String) -> AwsSqsClient(endpoint) }
        factory { (endpoint: String) -> AwsS3Client(endpoint) }
        factory { (endpoint: String) -> AwsDynamoDbClient(endpoint) }

        factory<(String) -> MessageRepository> {
            { endpoint ->
                MessageRepository(
                    snsClient = AwsSnsClient(endpoint),
                    sqsClient = AwsSqsClient(endpoint),
                    s3Client = AwsS3Client(endpoint),
                    dynamoDbClient = AwsDynamoDbClient(endpoint),
                    stepFunctionsClient = AwsStepFunctionsClient(endpoint),
                )
            }
        }

        single { UpdateRepository() }
        single { ServiceRepository(get()) }
        single { ServiceHealthRepository(get()) }
        single { SavedPayloadRepository() }

        single {
            RunningResourceRepository(
                sqsClientFactory = { endpoint -> AwsSqsClient(endpoint) },
                snsClientFactory = { endpoint -> AwsSnsClient(endpoint) },
                s3ClientFactory = { endpoint -> AwsS3Client(endpoint) },
                dynamoDbClientFactory = { endpoint -> AwsDynamoDbClient(endpoint) },
                stepFunctionsClientFactory = { endpoint -> AwsStepFunctionsClient(endpoint) },
                elastiCacheClientFactory = { endpoint -> AwsElastiCacheClient(endpoint) },
            )
        }
    }
