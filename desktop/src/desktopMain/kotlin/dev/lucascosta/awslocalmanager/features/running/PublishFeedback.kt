package dev.lucascosta.awslocalmanager.features.running

import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey

sealed class PublishFeedback {
    data class Success(val key: SuccessSnackbarKey) : PublishFeedback()

    data class Error(val message: String) : PublishFeedback()
}
