package com.tagantroy.remoteexecutionplugin.service

import build.bazel.remote.execution.v2.*;
import com.google.common.hash.Hashing
import com.google.protobuf.Duration
import com.google.protobuf.GeneratedMessageV3
import io.grpc.ManagedChannelBuilder
import java.io.File



fun createRemoteExecutionService(host: String): RemoteExecutionService {
    val channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext()
        .enableRetry()
        .build()
    val cas = ContentAddressableStorageGrpc.newBlockingStub(channel)
    val execution = ExecutionGrpc.newBlockingStub(channel)
    val actionCache = ActionCacheGrpc.newBlockingStub(channel)
    return RemoteExecutionService(cas, execution, actionCache)
}

class RemoteExecutionService(private val cas: ContentAddressableStorageGrpc.ContentAddressableStorageBlockingStub,
                             private val execution: ExecutionGrpc.ExecutionBlockingStub,
                             private val actionCache: ActionCacheGrpc.ActionCacheBlockingStub) {

    val digestUtil = DigestUtil(Hashing.sha256())

    private fun uploadToCas(message: GeneratedMessageV3): BatchUpdateBlobsResponse {
        val digest = digestUtil.compute(message)
        val actionUploadRequest = BatchUpdateBlobsRequest.Request.newBuilder().setData(message.toByteString()).setDigest(digest).build()
        val request = BatchUpdateBlobsRequest.newBuilder().addRequests(actionUploadRequest).build()
        return cas.batchUpdateBlobs(request)
    }

    fun execute(inputs: List<File>) {
        val command = Command.newBuilder()
//            .addAllArguments(listOf("echo","Hello World", ">", "test.txt"))
            .addAllArguments(listOf("eval 'echo \"\$NAME\"' > test.txt" ))
            .addEnvironmentVariables(Command.EnvironmentVariable.newBuilder().setName("NAME").build())
            .setPlatform(
                Platform.newBuilder()
                    .addProperties(
                        Platform.Property.newBuilder().setName("OSFamily").setValue("Linux").build()
                    )
                    .addProperties(
                        Platform.Property.newBuilder().setName("container-image").setValue("docker://marketplace.gcr.io/google/rbe-ubuntu16-04@sha256:f6568d8168b14aafd1b707019927a63c2d37113a03bcee188218f99bd0327ea1").build()
                    )
                    .build())
            .addOutputPaths("test.txt")
            .build()

        val commandDigest = digestUtil.compute(command)
        val inputRootPath = ""
        val actionInputRootDigest = digestUtil.compute(inputRootPath)

        val uploadCommandResponse = uploadToCas(command)

        val action = Action.newBuilder()
//            .setDoNotCache(true)
            .setInputRootDigest(actionInputRootDigest)
            .setTimeout(Duration.newBuilder().setSeconds(600).build())
            .setCommandDigest(commandDigest)
            .setPlatform(
                Platform.newBuilder()
                    .addProperties(
                        Platform.Property.newBuilder().setName("OSFamily").setValue("Linux").build()
                    )
                    .addProperties(
                        Platform.Property.newBuilder().setName("container-image").setValue("docker://marketplace.gcr.io/google/rbe-ubuntu16-04@sha256:f6568d8168b14aafd1b707019927a63c2d37113a03bcee188218f99bd0327ea1").build()
                    )
                    .build())
            .build()

        val actionDigest = digestUtil.compute(action)

        val uploadActionResponse = uploadToCas(action)

        val request =  ExecuteRequest.newBuilder().setInstanceName("remote-execution").setActionDigest(actionDigest).setSkipCacheLookup(true).build()
        val response = execution.execute(request)

        response.forEach {
            print("execute: ${it}" )
        }
        print("asdf")
        val req = GetActionResultRequest.newBuilder().setInstanceName("remote-execution").setActionDigest(actionDigest).build()
        val res = actionCache.getActionResult(req)
        print("asdf")
        res.stdoutDigest

        val r = cas.batchReadBlobs(BatchReadBlobsRequest.newBuilder().addDigests(res.stdoutDigest).build())
        print("sadfasdf")
    }
}