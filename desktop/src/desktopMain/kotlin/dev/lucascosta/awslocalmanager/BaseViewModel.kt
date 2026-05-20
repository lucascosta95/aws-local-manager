package dev.lucascosta.awslocalmanager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

abstract class BaseViewModel {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onCleared() {
        scope.cancel()
    }

    protected fun startPolling(
        intervalProvider: suspend () -> Long,
        action: suspend () -> Unit,
    ): Job =
        scope.launch {
            while (isActive) {
                action()
                delay(intervalProvider().seconds)
            }
        }
}
