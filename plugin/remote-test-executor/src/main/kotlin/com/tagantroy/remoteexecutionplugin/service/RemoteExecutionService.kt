package com.tagantroy.remoteexecutionplugin.service

import build.bazel.remote.execution.v2.*
import com.google.protobuf.Duration
import com.google.protobuf.GeneratedMessageV3
import com.tagantroy.remoteexecutionplugin.config.Config
import com.tagantroy.remoteexecutionplugin.internal.executer.RemoteTestExecuter
import io.grpc.ManagedChannelBuilder
import org.gradle.api.logging.Logging

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

    fun execute(arguments: List<String>, environment: Map<String, Any>, inputDigest: Digest, config: Config) {
        val capabilities =
            cabailities.getCapabilities(
                GetCapabilitiesRequest.newBuilder()
                    .setInstanceName(config.instance)
                    .build()
            )
        if (capabilities.executionCapabilities.digestFunction != DigestFunction.Value.SHA256) {
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
            .setPlatform(Platform.newBuilder().addAllProperties(config.platform.toProperties()).build())
            .addOutputPaths("report")
            .build()

        val commandDigest = digestUtil.compute(command)
        logger.info("Command digest: ${commandDigest.hash}")

        val uploadCommandResponse = uploadToCas(command)
        logger.info("upload command response: ${uploadCommandResponse.responsesList.first()}")

        logger.info("Build action")
        val action = Action.newBuilder()
            .setDoNotCache(config.doNotCache)
            .setInputRootDigest(inputDigest)
            .setTimeout(Duration.newBuilder().setSeconds(config.timeoutSeconds).build())
            .setCommandDigest(commandDigest)
            .setPlatform(Platform.newBuilder().addAllProperties(config.platform.toProperties()).build())
            .build()

        val actionDigest = digestUtil.compute(action)

        val uploadActionResponse = uploadToCas(action)
        logger.info("Upload action response: ${uploadActionResponse.responsesList.first()}")

        val request = ExecuteRequest.newBuilder()
            .setInstanceName(config.instance)
            .setActionDigest(actionDigest)
            .build()
        logger.info("Execute action")
        val response = execution.execute(request)

        response.forEach {
            logger.info("execute: $it")
        }
        val req = GetActionResultRequest.newBuilder()
            .setInstanceName(config.instance)
            .setActionDigest(actionDigest)
            .build()
        val res = actionCache.getActionResult(req)

        res.stdoutDigest
        val r = cas.batchReadBlobs(
            BatchReadBlobsRequest.newBuilder().addDigests(res.outputDirectoriesList.first().treeDigest).build()
        )
        r.responsesList.first()

        print("asdf")

    }
}