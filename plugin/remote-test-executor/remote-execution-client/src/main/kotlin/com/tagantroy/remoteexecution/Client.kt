package com.tagantroy.remoteexecution

import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc
import build.bazel.remote.execution.v2.ExecutionGrpc
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.MerkleTree
import com.tagantroy.remoteexecution.cas.CAS
import com.tagantroy.remoteexecution.config.Config
import com.tagantroy.remoteexecution.re.RemoteExecution
import com.tagantroy.types.Command
import com.tagantroy.types.ExecutionOptions
import io.grpc.ManagedChannelBuilder

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

    fun execute(command: Command, executionOptions: ExecutionOptions) {
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