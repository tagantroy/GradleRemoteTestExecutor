package com.tagantroy.merkletree.types

import build.bazel.remote.execution.v2.Directory
import com.tagantroy.merkletree.hash.toDigest
import java.nio.file.Path

fun fromBlob(byteArray: ByteArray): UploadInfoEntry {
    val digest = byteArray.toDigest()
    return UploadInfoEntry.Content(digest, byteArray)
}

fun fromPath(path: Path): UploadInfoEntry {
    val digest = path.toDigest()
    return UploadInfoEntry.Path(digest, path.toString())
}

fun fromProto(dir: Directory) : UploadInfoEntry {
    return fromBlob(dir.toByteArray())
}

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
