package com.tagantroy.merkletree.types

data class Digest(val hash: String, val size: Long)

fun emptyDigest(): Digest = Digest("", 0)