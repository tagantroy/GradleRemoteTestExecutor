package com.tagantroy.merkletree.types

sealed class FileSysNode {
    class EmptyDirectory : FileSysNode()
    data class File(val fileNode: FileNode): FileSysNode()
    data class Symlink(val symlink: SymlinkNode): FileSysNode()
}