package com.tagantroy.merkletree.types

data class TreeStats(
    var inputFiles: Int,
    var inputDirectories: Int,
    var inputSymlinks: Int,
    var totalInputBytes: Long
)