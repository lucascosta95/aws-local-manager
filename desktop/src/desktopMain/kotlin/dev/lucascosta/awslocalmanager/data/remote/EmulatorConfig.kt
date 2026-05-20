package dev.lucascosta.awslocalmanager.data.remote

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes

object EmulatorConfig {
    const val DEFAULT_ENDPOINT = "http://localhost:4566"
    const val ACCESS_KEY = "test"
    const val SECRET_KEY = "test"

    val credentialsProvider: CredentialsProvider =
        object : CredentialsProvider {
            override suspend fun resolve(attributes: Attributes) =
                Credentials(
                    accessKeyId = ACCESS_KEY,
                    secretAccessKey = SECRET_KEY,
                )
        }
}
