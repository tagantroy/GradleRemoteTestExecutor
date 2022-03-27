package com.tagantroy.types

enum class SymlinkBehaviorType {
    // UnspecifiedSymlinkBehavior means following clients.TreeSymlinkOpts
    // or DefaultTreeSymlinkOpts if clients.TreeSymlinkOpts is null.
    UnspecifiedSymlinkBehavior,
    // ResolveSymlink means symlinks are resolved.
    ResolveSymlink,
    // PreserveSymlink means symlinks are kept as-is.
    PreserveSymlink,
}