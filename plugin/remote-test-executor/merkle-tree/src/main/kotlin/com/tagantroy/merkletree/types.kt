package com.tagantroy.merkletree

data class TreeNode(
    val files: Map<String, FileNode>,
    val dirs: Map<String, TreeNode>,
    val symlinks: Map<String, SymlinkNode>
)

sealed class UploadInfoEntry(val digest: Digest /*, ueType: Int*/) {
    class Content(digest: Digest, val content: ByteArray) : UploadInfoEntry(digest)
    class Path(digest: Digest, val path: String) : UploadInfoEntry(digest)
}

data class Digest(val hash: String, val size: Long)
data class FileNode(val ue: UploadInfoEntry, val isExecutable: Boolean)
data class SymlinkNode(val target: String)

data class FileSysNode(
    val file: FileNode,
    val emptyDirectoryMarker: Boolean,
    val symlink: SymlinkNode
)

data class TreeStats(
    val inputFiles: Int,
    val inputDirectories: Int,
    val inputSymlinks: Int,
    val totalInputBytes: Int
)


data class TreeOutput(
    val digest: Digest,
    val path: String,
    val isExecutable: Boolean,
    val isEmptyDirectory: Boolean,
    val symlinkTarget: String
)


data class TreeSymlinkOpts(
    val preserved: Boolean,
    val followsTarget: Boolean
)