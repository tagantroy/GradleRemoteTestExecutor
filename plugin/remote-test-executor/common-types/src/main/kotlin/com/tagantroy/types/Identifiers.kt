package com.tagantroy.types

data class Identifiers(
    val commandId: String,
    val invocationId: String,
    val correlatedInvocationId: String,
    val toolName: String,
    val toolVersion: String,
    val executionId: String
)


