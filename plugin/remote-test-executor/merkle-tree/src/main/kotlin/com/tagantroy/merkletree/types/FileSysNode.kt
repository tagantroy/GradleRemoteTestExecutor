package com.tagantroy.merkletree.types

data class FileSysNode(
    val file: FileNode,
    val emptyDirectoryMarker: Boolean,
    val symlink: SymlinkNode
)