package com.tagantroy.merkletree.types

import com.tagantroy.merkletree.FileNode
import com.tagantroy.merkletree.SymlinkNode

data class TreeNode(
    val files: Map<String, FileNode>,
    val dirs: Map<String, TreeNode>,
    val symlinks: Map<String, SymlinkNode>
)