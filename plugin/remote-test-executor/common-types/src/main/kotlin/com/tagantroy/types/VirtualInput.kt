package com.tagantroy.types

class VirtualInput(
    val path: String,
    val contents: ByteArray,
    val isExecutable: Boolean,
    val isEmptyDirectory: Boolean,
) {
    override fun toString(): String {
        return "VirtualInput(path='$path', isExecutable=$isExecutable, isEmptyDirectory=$isEmptyDirectory)"
    }
}