package com.tagantroy.merkletree.types

data class TreeOutput(
    val digest: Digest,
    val path: String,
    val isExecutable: Boolean,
    val isEmptyDirectory: Boolean,
    val symlinkTarget: String
)