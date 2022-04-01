package com.tagantroy.merkletree.hash

import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.tagantroy.merkletree.types.Digest
import java.nio.file.Path

val hashFunction = Hashing.sha256()

fun Path.toDigest(): Digest {
    val hash = Files.asByteSource(this.toFile()).hash(hashFunction)
    val size = java.nio.file.Files.size(this)
    return Digest(hash.toString(), size)
}

fun ByteArray.toDigest(): Digest {
    val hasher = hashFunction.newHasher()
    hasher.putBytes(this)
    val hash = hasher.hash()
    return Digest(hash.toString(), this.size.toLong())
}