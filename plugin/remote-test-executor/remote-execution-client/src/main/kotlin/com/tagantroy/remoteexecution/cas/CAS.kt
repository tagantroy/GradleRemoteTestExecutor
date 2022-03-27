package com.tagantroy.remoteexecution.cas

import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc

class CAS(cas: ContentAddressableStorageGrpc.ContentAddressableStorageStub) {
    fun uploadIfMissing() {
        TODO("Not implemented yet")
    }

    fun batchWriteBlobs() {
        TODO("Not implemented yet")
    }

    fun batchDownloadBlobs() {
        TODO("Not implemented yet")
    }
}