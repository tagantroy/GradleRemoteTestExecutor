package com.tagantroy.remoteexecution.re

import build.bazel.remote.execution.v2.ExecutionGrpc

class RemoteExecution(private val remoteExecution: ExecutionGrpc.ExecutionBlockingStub) {
    fun execute() {

    }
}