package com.tagantroy.remoteexecutionplugin.service

import build.bazel.remote.execution.v2.Action
import build.bazel.remote.execution.v2.Command
import build.bazel.remote.execution.v2.Digest
import build.bazel.remote.execution.v2.Directory
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readBytes

val SHA256 = DigestUtil(Hashing.sha256())

class DigestUtil(private val hashFunction: HashFunction) {
    fun compute(content: Command): Digest {
        return compute(content.toByteString())
    }

    fun compute(content: Action): Digest {
        return compute(content.toByteString())
    }

    fun compute(content: GeneratedMessageV3): Digest {
        return compute(content.toByteString())
    }

    fun compute(hashCode: HashCode, sizeBytes: Long): Digest {
        return Digest.newBuilder().setHash(hashCode.toString()).setSizeBytes(sizeBytes).build()
    }

    @OptIn(ExperimentalPathApi::class)
    fun hash(path: Path): HashCode {
        return hashFunction.hashBytes(path.readBytes())
    }

    fun compute(content: ByteString): Digest {
        val array = content.toByteArray()
        val hash = hashFunction.hashBytes(array).toString()
        return Digest.newBuilder()
            .setHash(hash)
            .setSizeBytes(content.size().toLong())
            .build()
    }

    fun compute(content: String): Digest {
        val array = content.encodeToByteArray()
        val hash = hashFunction.hashBytes(array)

        return Digest.newBuilder().setHash(hash.toString()).setSizeBytes(array.size.toLong()).build()
    }

    fun hashUnordered(filterNotNull: List<HashCode>): HashCode {
        return Hashing.combineUnordered(filterNotNull)
    }

    fun hash(path: String): HashCode {
        return hashFunction.hashString(path, Charset.defaultCharset())
    }

    fun compute(hash: HashCode): Digest {
        return compute(hash.toString())
    }

    fun compute(dir: Directory, sizeBytes: Long): Digest {
        return Digest.newBuilder().setHash(hashFunction.hashBytes(dir.toByteArray()).toString()).setSizeBytes(sizeBytes)
            .build()
    }
}