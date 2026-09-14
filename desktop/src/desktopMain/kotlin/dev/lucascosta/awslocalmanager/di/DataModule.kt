package dev.lucascosta.awslocalmanager.di

import dev.lucascosta.awslocalmanager.data.remote.AwsDynamoDbClient
import dev.lucascosta.awslocalmanager.data.remote.AwsElastiCacheClient
import dev.lucascosta.awslocalmanager.data.remote.AwsS3Client
import dev.lucascosta.awslocalmanager.data.remote.AwsSnsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsSqsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsSsmClient
import dev.lucascosta.awslocalmanager.data.remote.AwsStepFunctionsClient
import dev.lucascosta.awslocalmanager.data.remote.EmulatorClient
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
import dev.lucascosta.awslocalmanager.data.repository.MessageRepository
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.data.repository.RunningResourceRepository
import dev.lucascosta.awslocalmanager.data.repository.SavedPayloadRepository
import dev.lucascosta.awslocalmanager.data.repository.ServiceHealthRepository
import dev.lucascosta.awslocalmanager.data.repository.ServiceRepository
import dev.lucascosta.awslocalmanager.data.repository.SkillCatalogRepository
import dev.lucascosta.awslocalmanager.data.repository.SkillStateRepository
import dev.lucascosta.awslocalmanager.data.repository.UpdateRepository
import org.koin.dsl.module

val dataModule =
    module {
        single { PreferencesRepository() }
        single { EmulatorClient() }
        single { KafkaBrokerClient() }

        factory { (endpoint: String) -> AwsSnsClient(endpoint) }
        factory { (endpoint: String) -> AwsSqsClient(endpoint) }
        factory { (endpoint: String) -> AwsS3Client(endpoint) }
        factory { (endpoint: String) -> AwsDynamoDbClient(endpoint) }

        factory<(String) -> MessageRepository> {
            val kafkaBrokerClient: KafkaBrokerClient = get()
            val createRepository: (String) -> MessageRepository = { endpoint ->
                MessageRepository(
                    snsClient = AwsSnsClient(endpoint),
                    sqsClient = AwsSqsClient(endpoint),
                    s3Client = AwsS3Client(endpoint),
                    dynamoDbClient = AwsDynamoDbClient(endpoint),
                    stepFunctionsClient = AwsStepFunctionsClient(endpoint),
                    kafkaBrokerClient = kafkaBrokerClient,
                )
            }
            createRepository
        }

        single { UpdateRepository() }
        single { ServiceRepository(get()) }
        single { ServiceHealthRepository(get()) }
        single { SavedPayloadRepository() }
        single { SkillCatalogRepository() }
        single { SkillStateRepository() }

        single {
            RunningResourceRepository(
                sqsClientFactory = { endpoint -> AwsSqsClient(endpoint) },
                snsClientFactory = { endpoint -> AwsSnsClient(endpoint) },
                s3ClientFactory = { endpoint -> AwsS3Client(endpoint) },
                dynamoDbClientFactory = { endpoint -> AwsDynamoDbClient(endpoint) },
                stepFunctionsClientFactory = { endpoint -> AwsStepFunctionsClient(endpoint) },
                elastiCacheClientFactory = { endpoint -> AwsElastiCacheClient(endpoint) },
                ssmClientFactory = { endpoint -> AwsSsmClient(endpoint) },
            )
        }
    }
