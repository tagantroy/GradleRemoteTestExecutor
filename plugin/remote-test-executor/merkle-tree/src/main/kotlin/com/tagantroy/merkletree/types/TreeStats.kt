package com.tagantroy.merkletree.types

data class TreeStats(
    val inputFiles: Int,
    val inputDirectories: Int,
    val inputSymlinks: Int,
    val totalInputBytes: Int
)