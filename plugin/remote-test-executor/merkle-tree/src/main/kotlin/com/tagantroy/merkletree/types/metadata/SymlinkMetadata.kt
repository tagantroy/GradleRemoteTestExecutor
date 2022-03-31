package com.tagantroy.merkletree.types.metadata

data class SymlinkMetadata(
    val target: String,
    val isDangling: Boolean
)