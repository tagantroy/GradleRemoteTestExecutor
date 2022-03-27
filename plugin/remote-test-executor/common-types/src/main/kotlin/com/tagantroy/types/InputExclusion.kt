package com.tagantroy.types

data class InputExclusion(val regex: Regex, val type: Type)

enum class Type {
    // UnspecifiedInputType means any input type will match.
    UnspecifiedInputType,

    // DirectoryInputType means only directories match.
    DirectoryInputType,

    // FileInputType means only files match.
    FileInputType,

    // SymlinkInputType means only symlink match.
    SymlinkInputType,
}