package com.tagantroy.remoteexecutionplugin.service
import build.bazel.remote.execution.v2.Action;
import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.Command;
import build.bazel.remote.execution.v2.DigestFunction;
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3

class DigestUtil(private val hashFunction: HashFunction) {
    fun compute(content: Command) : Digest {
        return compute(content.toByteString())
    }
    fun compute(content: Action) : Digest {
        return compute(content.toByteString())
    }
    fun compute(content: GeneratedMessageV3): Digest {
        return compute(content.toByteString())
    }
    fun compute(content: ByteString) : Digest {
        val array = content.toByteArray()
        val hash = hashFunction.hashBytes(array).toString()
        return Digest.newBuilder()
            .setHash(hash)
            .setSizeBytes(array.size.toLong())
            .build()
    }
    fun compute(content: String) : Digest {
        val array = content.encodeToByteArray()
        val hash = hashFunction.hashBytes(array).toString()
        return Digest.newBuilder().setHash(hash).setSizeBytes(array.size.toLong()).build()
    }
}