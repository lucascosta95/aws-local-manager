package dev.lucascosta.awslocalmanager.data.remote

data class ProcessOutput(val stdout: String, val stderr: String, val exitCode: Int)
