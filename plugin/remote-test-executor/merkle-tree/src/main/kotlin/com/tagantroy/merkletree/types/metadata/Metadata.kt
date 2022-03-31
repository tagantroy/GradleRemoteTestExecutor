package com.tagantroy.merkletree.types.metadata

import com.tagantroy.merkletree.types.Digest

data class Metadata(
    val digest: Digest,
    val isExecutable: Boolean,
    val isDirectory: Boolean,
    val isRegularFile: Boolean,
    val isSymlink: Boolean,
//    val mTime: time.Time
    val symlink: SymlinkMetadata?
)