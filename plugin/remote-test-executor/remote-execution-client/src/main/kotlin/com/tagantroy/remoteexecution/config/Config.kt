package com.tagantroy.remoteexecution.config

data class Config(
    /**
     * The remote execution service to dial when calling via gRPC, including port, such as 'localhost:8790' or 'remotebuildexecution.googleapis.com:443'
     */
    val service: String,
    /**
     * CASService represents the host (and, if applicable, port) of the CAS service, if different from the remote execution service.
     */
    val casService: String,
)