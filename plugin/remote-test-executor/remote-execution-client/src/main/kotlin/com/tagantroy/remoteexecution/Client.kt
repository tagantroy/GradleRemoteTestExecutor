package com.tagantroy.remoteexecution

import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc
import build.bazel.remote.execution.v2.ExecutionGrpc
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.MerkleTree
import com.tagantroy.remoteexecution.cas.CAS
import com.tagantroy.remoteexecution.config.Config
import com.tagantroy.remoteexecution.re.RemoteExecution
import io.grpc.ManagedChannelBuilder

data class Request(
    /**
     * An identifier for the command for debugging.
     */
    val commandId: String = "",
    /**
     * An identifier for a group of commands for debugging.
     */
    val invocationId: String = "",
    /**
     * The name of the tool to associate with executed commands.
     */
    val toolName: String = "",
    /**
     * The exec root of the command. The path from which all inputs and outputs are defined relatively.
     */
    val execRoot: String = "",
    /**
     * The working directory, relative to the exec root, for the command to run in.
     * It must be a directory which exists in the input tree.
     * If it is left empty, then the action is run in the exec root.
     */
    val workingDir: String = "",
    /**
     * Comma-separated command input paths, relative to exec root.
     */
    val inputs: List<String> = emptyList(),
    /**
     * Comma-separated command output file paths, relative to exec root.
     */
    val outputFiles: List<String> = emptyList(),
    /**
     * Comma-separated command output directory paths, relative to exec root.
     */
    val outputDirs: List<String> = emptyList(),
    /**
     * Timeout for the command. Value of 0 means no timeout.
     */
    val execTimeout: Long = 0,
    /**
     * Comma-separated key value pairs in the form key=value. This is used to identify remote platform settings like the docker image to use to run the command.
     */
    val platform: Map<String, String> = emptyMap(),
    /**
     * Environment variables to pass through to remote execution, as comma-separated key value pairs in the form key=value.
     */
    val environmentVariables: Map<String, String> = emptyMap(),
    /**
     * Boolean indicating whether to accept remote cache hits.
     */
    val acceptCached: Boolean = true,
    /**
     * Boolean indicating whether to skip caching the command result remotely.
     */
    val doNotCache: Boolean = false,
    /**
     * Boolean indicating whether to download outputs after the command is executed.
     */
    val downloadOutputs: Boolean = true,
    /**
     * Boolean indicating whether to download stdout and stderr after the command is executed.
     */
    val downloadOutErr: Boolean = true,
)


class Client(val remoteExecution: RemoteExecution, private val cas: CAS) {
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

    fun execute(command: String, request: Request) {
        val inputs = computeInputs()
        val merkleTree = MerkleTree()
        remoteExecution.execute()
    }

    private fun computeInputs() {
        logger.error("Prepare command");
        val cmdId = ""
        val executionId = ""
        val command = ""
        logger.error("$cmdId $executionId > Command: \n $command")
        val grpcCommand = ""
        val cmdDigest = ""
        logger.error("$cmdId $executionId > Command digest: $cmdDigest")
        logger.error("$cmdId $executionId > Computing input Merkle tree...")
        val tree = MerkleTree().computeMerkleTree("", "", "");
        val actionDigest = ""
        logger.error("$cmdId $executionId > Action digest: $actionDigest")
    }
}