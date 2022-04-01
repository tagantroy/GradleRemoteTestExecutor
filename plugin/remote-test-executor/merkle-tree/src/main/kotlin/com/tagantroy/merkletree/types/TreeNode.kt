package com.tagantroy.merkletree.types

data class TreeNode(
    val files: MutableMap<String, FileNode> = hashMapOf(),
    val dirs: MutableMap<String, TreeNode> = hashMapOf(),
    val symlinks: MutableMap<String, SymlinkNode> = hashMapOf(),
)