package com.tagantroy.remoteexecution.cas

import build.bazel.remote.execution.v2.BatchUpdateBlobsRequest
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc
import build.bazel.remote.execution.v2.Digest
import build.bazel.remote.execution.v2.FindMissingBlobsRequest
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.types.UploadInfoEntry
import com.tagantroy.remoteexecution.Client

class CAS(private val cas: ContentAddressableStorageGrpc.ContentAddressableStorageBlockingStub) {
    private val logger = LoggerFactory.getLogger(Client::class.java)
    fun findMissing(digests: List<Digest>): List<Digest> {
        val req = FindMissingBlobsRequest.newBuilder()
            .addAllBlobDigests(digests).build()
        return cas.findMissingBlobs(req).missingBlobDigestsList
    }

    fun uploadIfMissing(blobs: Map<Digest, UploadInfoEntry>, digests: List<Digest>) {
        val missing = findMissing(digests)
        val req = BatchUpdateBlobsRequest.newBuilder()
            .addAllRequests(missing.map {
                BatchUpdateBlobsRequest.Request.newBuilder()
                    .setDigest(it)
//                    .setCompressor()
//                    .setCompressorValue()
//                    .setData(ByteString.copyFrom())
                    .build()
            }).build()
        logger.error("BatchUpdateBlobs")
        cas.batchUpdateBlobs(req)
    }

    fun batchWriteBlobs() {
    }

    fun batchDownloadBlobs() {
    }
}