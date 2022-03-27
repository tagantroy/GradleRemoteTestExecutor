package com.tagantroy.merkletree.types

data class TreeNode(
    val files: Map<String, FileNode>,
    val dirs: Map<String, TreeNode>,
    val symlinks: Map<String, SymlinkNode>
)