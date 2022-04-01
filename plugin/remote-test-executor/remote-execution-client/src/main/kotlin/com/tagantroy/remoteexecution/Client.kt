package com.tagantroy.remoteexecution

import build.bazel.remote.execution.v2.Command.EnvironmentVariable
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc
import build.bazel.remote.execution.v2.ExecutionGrpc
import build.bazel.remote.execution.v2.Platform
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.MerkleTree
import com.tagantroy.merkletree.cache.SimpleCache
import com.tagantroy.merkletree.types.Digest
import com.tagantroy.remoteexecution.cas.CAS
import com.tagantroy.remoteexecution.config.Config
import com.tagantroy.remoteexecution.re.RemoteExecution
import com.tagantroy.types.Command
import com.tagantroy.types.ExecutionOptions
import com.tagantroy.types.InputSpec
import io.grpc.ManagedChannelBuilder

class Client(private val remoteExecution: RemoteExecution, private val cas: CAS) {
    private val logger = LoggerFactory.getLogger(Client::class.java)

    companion object {
        fun fromConfig(config: Config): Client {
            val casChannel = ManagedChannelBuilder
                .forTarget(config.casService)
                .usePlaintext()
                .build()
            val casGRPC = ContentAddressableStorageGrpc.newStub(casChannel)
            val cas = CAS(casGRPC)
            val reChannel = ManagedChannelBuilder
                .forTarget(config.service)
                .usePlaintext()
                .build()
            val reGRPC = ExecutionGrpc.newStub(reChannel)
            val re = RemoteExecution(reGRPC)
            return Client(re, cas)
        }
    }

    fun execute(command: Command, executionOptions: ExecutionOptions) {
        val inputs = computeInputs(command)

//        remoteExecution.execute()
    }

    private fun computeInputs(cmd: Command) {
        logger.error("Prepare command");
        val cmdId = cmd.identifiers.commandId
        val executionId = cmd.identifiers.executionId
        val commandPb = cmd.toPb()
        logger.error("$cmdId $executionId > Command: \n $commandPb")
        val grpcCommand = ""
        val cmdDigest = ""
        logger.error("$cmdId $executionId > Command digest: $cmdDigest")
        logger.error("$cmdId $executionId > Computing input Merkle tree...")
        val simpleCache = SimpleCache()
        val merkleTree = MerkleTree(cmd.inputSpec, simpleCache)
        val (root, inputs, stats) = merkleTree.computeMerkleTree(
            cmd.execRoot,
            cmd.workingDir,
            cmd.remoteWorkingDir
        )
        val actionDigest = ""
        logger.error("$cmdId $executionId > Action digest: $actionDigest")
    }
}

private fun Command.toPb(): build.bazel.remote.execution.v2.Command {
    val command = this
    return build.bazel.remote.execution.v2.Command.newBuilder().apply {
        addAllArguments(command.args)
        addAllEnvironmentVariables(command.inputSpec.environmentVariable.entries.map {
            EnvironmentVariable.newBuilder().setName(it.key).setValue(it.value).build()
        })
        addAllOutputFiles(command.outputFiles)
        addAllOutputPaths(command.outputFiles)
        addAllOutputPaths(command.outputFiles + command.outputDirs)
        platform = Platform.newBuilder().apply {
            addAllProperties(command.platform.map {
                Platform.Property.newBuilder().setName(it.key).setValue(it.value).build()
            })
        }.build()
        workingDirectory = command.workingDir
    }.build()
}
