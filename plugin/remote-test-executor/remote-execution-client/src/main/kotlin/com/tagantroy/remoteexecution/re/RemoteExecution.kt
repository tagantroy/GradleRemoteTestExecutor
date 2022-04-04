package com.tagantroy.remoteexecution.re

import build.bazel.remote.execution.v2.Digest
import build.bazel.remote.execution.v2.ExecuteRequest
import build.bazel.remote.execution.v2.ExecuteResponse
import build.bazel.remote.execution.v2.ExecutionGrpc

class RemoteExecution(private val remoteExecution: ExecutionGrpc.ExecutionBlockingStub) {
    fun execute(actionDigest: Digest): ExecuteResponse {
        val req = ExecuteRequest.newBuilder()
            .setActionDigest(actionDigest)
            .setInstanceName("")
            .setSkipCacheLookup(true)
            .build()
        val res = remoteExecution.execute(req)
        while (res.hasNext()) {
            val cur = res.next()
            if (cur.done) {
                return cur.response.unpack(ExecuteResponse::class.java)
            }
        }
        throw RuntimeException()
    }
}