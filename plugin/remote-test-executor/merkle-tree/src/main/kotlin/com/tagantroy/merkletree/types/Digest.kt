package com.tagantroy.merkletree.types

import build.bazel.remote.execution.v2.Digest as REDigest

data class Digest(val hash: String, val size: Long)

fun emptyDigest(): Digest = Digest("", 0)

fun Digest.toProto(): REDigest {
    return REDigest.newBuilder()
        .setHash(this.hash)
        .setSizeBytes(this.size)
        .build()
}