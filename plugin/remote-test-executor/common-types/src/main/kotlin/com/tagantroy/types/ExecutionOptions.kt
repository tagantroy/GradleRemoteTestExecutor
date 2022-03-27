package com.tagantroy.types

data class ExecutionOptions(
    // Whether to accept cached action results. Defaults to true.
    val acceptCached: Boolean,
    // When set, this execution results will not be cached.
    val doNotCache: Boolean,
    // Download command outputs after execution. Defaults to true.
    val downloadOutputs: Boolean,
    // Download command stdout and stderr. Defaults to true.
    val downloadOutErr: Boolean
)