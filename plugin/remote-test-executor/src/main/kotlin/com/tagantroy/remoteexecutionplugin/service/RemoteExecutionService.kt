package com.tagantroy.remoteexecutionplugin.service

import build.bazel.remote.execution.v2.*;
import com.google.common.hash.Hashing
import com.google.protobuf.Duration
import com.google.protobuf.GeneratedMessageV3
import com.tagantroy.remoteexecutionplugin.internal.executer.RemoteTestExecuter
import io.grpc.ManagedChannelBuilder
import org.gradle.api.logging.Logging
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Path

fun createRemoteExecutionService(host: String): RemoteExecutionService {
    val channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext()
        .enableRetry()
        .build()
    val cas = ContentAddressableStorageGrpc.newBlockingStub(channel)
    val execution = ExecutionGrpc.newBlockingStub(channel)
    val actionCache = ActionCacheGrpc.newBlockingStub(channel)
    val capabilities = CapabilitiesGrpc.newBlockingStub(channel)
    return RemoteExecutionService(cas, execution, actionCache, capabilities)
}

class RemoteExecutionService(
    private val cas: ContentAddressableStorageGrpc.ContentAddressableStorageBlockingStub,
    private val execution: ExecutionGrpc.ExecutionBlockingStub,
    private val actionCache: ActionCacheGrpc.ActionCacheBlockingStub,
    private val cabailities: CapabilitiesGrpc.CapabilitiesBlockingStub
) {

    private val digestUtil = SHA256
    private val logger = Logging.getLogger(RemoteTestExecuter::class.java)

    fun upload(request: BatchUpdateBlobsRequest): BatchUpdateBlobsResponse {
        return cas.batchUpdateBlobs(request)
    }

    private fun uploadToCas(message: GeneratedMessageV3): BatchUpdateBlobsResponse {
        logger.info("Upload to cas")
        val digest = digestUtil.compute(message)
        val actionUploadRequest =
            BatchUpdateBlobsRequest.Request.newBuilder().setData(message.toByteString()).setDigest(digest).build()
        val request = BatchUpdateBlobsRequest.newBuilder().addRequests(actionUploadRequest).build()
        return upload(request)
    }

    fun execute(arguments: List<String>, environment: Map<String, Any>, inputDigest: Digest) {
        val capabilities =
            cabailities.getCapabilities(GetCapabilitiesRequest.newBuilder().setInstanceName("remote-execution").build())
        if(capabilities.executionCapabilities.digestFunction != DigestFunction.Value.SHA256){
            throw RuntimeException("GOOOSH")
        }
        logger.info("Capabilities: $capabilities")
        logger.info("Execute action with arguments: $arguments")
        logger.info("Execute action with environment: $environment")
        val command = Command.newBuilder()
            .addAllArguments(arguments)
            .addAllEnvironmentVariables(environment.entries.map {
                Command.EnvironmentVariable.newBuilder()
                    .setName(it.key)
                    .setValue(it.value.toString())
                    .build()
            })
            .setPlatform(
                Platform.newBuilder()
                    .addProperties(
                        Platform.Property.newBuilder().setName("OSFamily").setValue("Linux").build()
                    )
                    .addProperties(
                        Platform.Property.newBuilder().setName("container-image")
                            .setValue("docker://marketplace.gcr.io/google/rbe-ubuntu16-04@sha256:f6568d8168b14aafd1b707019927a63c2d37113a03bcee188218f99bd0327ea1")
                            .build()
                    )
                    .build()
            )
            .addOutputPaths("test.txt")
            .build()

        val commandDigest = digestUtil.compute(command)
        logger.info("Command digest: ${commandDigest.hash}")


        val uploadCommandResponse = uploadToCas(command)
        logger.info("Build action")
        val action = Action.newBuilder()
            .setDoNotCache(true)
            .setInputRootDigest(Digest.newBuilder(inputDigest).setSizeBytes(64).build())
            .setTimeout(Duration.newBuilder().setSeconds(600).build())
            .setCommandDigest(commandDigest)
            .setPlatform(
                Platform.newBuilder()
                    .addProperties(
                        Platform.Property.newBuilder().setName("OSFamily").setValue("Linux").build()
                    )
                    .addProperties(
                        Platform.Property.newBuilder().setName("container-image")
                            .setValue("docker://marketplace.gcr.io/google/rbe-ubuntu16-04@sha256:f6568d8168b14aafd1b707019927a63c2d37113a03bcee188218f99bd0327ea1")
                            .build()
                    )
                    .build()
            )
            .build()

        val actionDigest = digestUtil.compute(action)

        val uploadActionResponse = uploadToCas(action)

        val request = ExecuteRequest.newBuilder().setInstanceName("remote-execution").setActionDigest(actionDigest)
            .setSkipCacheLookup(true).build()
        logger.info("Execute action")
        val response = execution.execute(request)

        response.forEach {
            print("execute: ${it}")
        }
        print("asdf")
        val req = GetActionResultRequest.newBuilder().setInstanceName("remote-execution").setActionDigest(actionDigest)
            .build()
        val res = actionCache.getActionResult(req)
        print("asdf")
        res.stdoutDigest

        val r = cas.batchReadBlobs(BatchReadBlobsRequest.newBuilder().addDigests(res.stdoutDigest).build())
    }
}