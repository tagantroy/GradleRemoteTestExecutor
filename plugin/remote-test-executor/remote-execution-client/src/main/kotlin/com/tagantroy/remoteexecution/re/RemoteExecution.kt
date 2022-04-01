package com.tagantroy.remoteexecution.re

import build.bazel.remote.execution.v2.Digest
import build.bazel.remote.execution.v2.ExecuteRequest
import build.bazel.remote.execution.v2.ExecutionGrpc

class RemoteExecution(private val remoteExecution: ExecutionGrpc.ExecutionBlockingStub) {
    fun execute(actionDigest: Digest) {
        val req = ExecuteRequest.newBuilder()
            .setActionDigest(actionDigest)
            .setInstanceName("")
            .setSkipCacheLookup(true)
            .build()
        remoteExecution.execute(req).forEach {
            if (it.done) {
                println("${it.name} DONE!")
            }
        }
    }
}