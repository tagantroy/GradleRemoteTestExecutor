package com.tagantroy.remoteexecution

import build.bazel.remote.execution.v2.*
import build.bazel.remote.execution.v2.Command.EnvironmentVariable
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.MerkleTree
import com.tagantroy.merkletree.cache.SimpleCache
import com.tagantroy.merkletree.types.Digest
import com.tagantroy.merkletree.types.UploadInfoEntry
import com.tagantroy.merkletree.types.fromProto
import com.tagantroy.merkletree.types.toProto
import com.tagantroy.remoteexecution.cas.CAS
import com.tagantroy.remoteexecution.config.Config
import com.tagantroy.remoteexecution.re.RemoteExecution
import com.tagantroy.types.Command
import com.tagantroy.types.ExecutionOptions
import io.grpc.ManagedChannelBuilder
import java.io.File
import java.nio.file.Path

class Client(
    private val remoteExecution: RemoteExecution,
    private val cas: CAS,
    val capabilities: CapabilitiesGrpc.CapabilitiesBlockingStub
) {
    private val logger = LoggerFactory.getLogger(Client::class.java)

    companion object {
        fun fromConfig(config: Config): Client {
            val casChannel = ManagedChannelBuilder
                .forTarget(config.casService)
                .usePlaintext()
                .build()
            val casGRPC = ContentAddressableStorageGrpc.newBlockingStub(casChannel)
            val cas = CAS(casGRPC)
            val reChannel = ManagedChannelBuilder
                .forTarget(config.service)
                .usePlaintext()
                .build()
            val reGRPC = ExecutionGrpc.newBlockingStub(reChannel)
            val re = RemoteExecution(reGRPC)
            val capabilities = CapabilitiesGrpc.newBlockingStub(reChannel)
            return Client(re, cas, capabilities)
        }
    }

    fun execute(command: Command, executionOptions: ExecutionOptions): ExecuteResponse {
        val cmdId = command.identifiers.commandId
        val executionId = command.identifiers.executionId
        logger.error("$cmdId $executionId > Compute inputs")
        val (actionDigest, inputs) = computeInputs(command)
        logger.error("$cmdId $executionId > Upload if missing")
        cas.uploadIfMissing(inputs)
        return remoteExecution.execute(actionDigest.toProto())
    }

    private data class ComputeInputsResponse(
        val actionDigest: Digest,
        val uploadInfoEntry: Map<Digest, UploadInfoEntry>
    )

    private fun computeInputs(cmd: Command): ComputeInputsResponse {
        logger.error("Prepare command");
        val cmdId = cmd.identifiers.commandId
        val executionId = cmd.identifiers.executionId
        val commandPb = cmd.toPb()
        logger.error("$cmdId $executionId > Command: \n $commandPb")
        val cmdPbUe = fromProto(commandPb)
        val cmdDigest = cmdPbUe.digest
        logger.error("$cmdId $executionId > Command digest: $cmdDigest")
        logger.error("$cmdId $executionId > Computing input Merkle tree...")
        val simpleCache = SimpleCache()
        val merkleTree = MerkleTree(cmd.inputSpec, simpleCache)
        val (root, inputs, stats) = merkleTree.computeMerkleTree(
            cmd.execRoot,
            cmd.workingDir,
            cmd.remoteWorkingDir
        )
        val actionPb = Action.newBuilder()
            .setCommandDigest(cmdDigest.toProto())
            .setInputRootDigest(root.toProto())
            .setDoNotCache(true) //TODO: Fix it later
            .build()
        val actionPbUe = fromProto(actionPb)
        val actionDigest = actionPbUe.digest
        logger.error("$cmdId $executionId > Action digest: $actionDigest")
        val combinedBlobs = inputs + mapOf(cmdPbUe.digest to cmdPbUe, actionPbUe.digest to actionPbUe)
        return ComputeInputsResponse(actionDigest, combinedBlobs)
    }

    fun downloadFile(digest: build.bazel.remote.execution.v2.Digest): ByteArray {
        return cas.downloadFile(digest)
    }

    fun downloadOutputDirectory(root: OutputDirectory, dest: Path) {
        val res = cas.downloadDirectory(root.treeDigest)
        val destDir = dest.toFile()
        destDir.mkdir()
        res.forEach {
            File(destDir, it.path).writeBytes(it.data)
        }
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
