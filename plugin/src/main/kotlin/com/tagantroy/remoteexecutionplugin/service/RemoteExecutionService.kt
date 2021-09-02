package com.tagantroy.remoteexecutionplugin.service

import build.bazel.remote.execution.v2.*;
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder

data class Config(val host: String) {

}

class RemoteExecutionService(val grpcConfig: Config) {
    fun upload() {
        val channel = ManagedChannelBuilder.forTarget(grpcConfig.host).build()
        val cas = ContentAddressableStorageGrpc.newBlockingStub(channel)
        val request = BatchUpdateBlobsRequest.newBuilder().build()
        val response = cas.batchUpdateBlobs(request)
    }

    fun execute() {

    }
}