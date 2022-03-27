package com.tagantroy.merkletree.types

import com.tagantroy.merkletree.Digest

sealed class UploadInfoEntry(val digest: Digest) {
    class Content(digest: Digest, val content: ByteArray) : UploadInfoEntry(digest)
    class Path(digest: Digest, val path: String) : UploadInfoEntry(digest)

    fun isFile(): Boolean {
        return when (this) {
            is Path -> true
            is Content -> false
        }
    }

    fun isBlob(): Boolean {
        return when (this) {
            is Path -> false
            is Content -> true
        }
    }
}
