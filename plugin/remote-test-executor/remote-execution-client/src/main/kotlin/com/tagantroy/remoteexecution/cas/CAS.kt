package com.tagantroy.remoteexecution.cas

import build.bazel.remote.execution.v2.BatchUpdateBlobsRequest
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc
import build.bazel.remote.execution.v2.Digest
import build.bazel.remote.execution.v2.FindMissingBlobsRequest
import com.google.protobuf.ByteString
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.types.UploadInfoEntry
import com.tagantroy.merkletree.types.toProto
import java.nio.file.Files
import java.nio.file.Paths

class CAS(private val cas: ContentAddressableStorageGrpc.ContentAddressableStorageBlockingStub) {
    private val logger = LoggerFactory.getLogger(CAS::class.java)

    private fun findMissing(digests: List<Digest>): List<Digest> {
        val req = FindMissingBlobsRequest.newBuilder()
            .addAllBlobDigests(digests).build()
        return cas.findMissingBlobs(req).missingBlobDigestsList
    }

    fun uploadIfMissing(blobs: Map<com.tagantroy.merkletree.types.Digest, UploadInfoEntry>) {
        val digests = blobs.keys.map { it.toProto() }
        val missing = findMissing(digests)
        if (missing.isEmpty()) return
        val req = BatchUpdateBlobsRequest.newBuilder()
            .addAllRequests(missing.map {
                BatchUpdateBlobsRequest.Request.newBuilder().apply {
                    setDigest(it)
                    val ue = blobs[com.tagantroy.merkletree.types.Digest(it.hash, it.sizeBytes)]
                    when (ue) {
                        is UploadInfoEntry.Content -> {
                            setData(ByteString.copyFrom(ue.content))
                        }
                        is UploadInfoEntry.Path -> {
                            setData(ByteString.readFrom(Files.newInputStream(Paths.get(ue.path))))
                        }
                    }
                }.build()
            }).build()
        cas.batchUpdateBlobs(req)
    }

    fun batchWriteBlobs() {
    }

    fun batchDownloadBlobs() {
    }
}
