package com.tagantroy.types

class VirtualInput(
    val path: String,
    val contents: ByteArray,
    val isExecutable: Boolean,
    val isEmptyDirectory: Boolean,
)