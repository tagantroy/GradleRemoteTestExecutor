package com.tagantroy.remoteexecution.cas

import build.bazel.remote.execution.v2.*
import com.google.protobuf.ByteString
import com.sun.org.slf4j.internal.LoggerFactory
import com.tagantroy.merkletree.types.UploadInfoEntry
import com.tagantroy.merkletree.types.toProto
import java.nio.file.Files
import java.nio.file.Path
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

    fun downloadDirectory(rootDigest: Digest): List<DownloadedFiles> {
        val fetchDirectoryInfoReq = BatchReadBlobsRequest.newBuilder()
            .addDigests(rootDigest)
            .setInstanceName("")
            .build()
        val fetchDirInfoResponse = cas.batchReadBlobs(fetchDirectoryInfoReq)
        val tree = Tree.parseFrom(fetchDirInfoResponse.responsesList.first().data)
        val rootDirectory = tree.root
        val filesMap = rootDirectory.filesList.map { it.digest to it.name }.toMap()
        val fetchFilesRequest = BatchReadBlobsRequest.newBuilder()
            .addAllDigests(filesMap.keys)
            .setInstanceName("")
            .build()
        val fetchFilesResponse = cas.batchReadBlobs(fetchFilesRequest)
        return fetchFilesResponse.responsesList.map {
            DownloadedFiles(filesMap[it.digest]!!, it.data.toByteArray())
        }
    }

    data class DownloadedFiles(
        val path: String,
        val data: ByteArray
    )


    fun flattenTree() {

    }

    fun downloadFile(digest: Digest): ByteArray {
        val req = BatchReadBlobsRequest.newBuilder()
            .setInstanceName("")
            .addDigests(digest)
            .build()
        val res = cas.batchReadBlobs(req)
        val downloadedFile = res.responsesList.first()
        return downloadedFile.data.toByteArray()
    }

}
