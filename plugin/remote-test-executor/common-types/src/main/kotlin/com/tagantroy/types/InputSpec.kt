package com.tagantroy.types

data class InputSpec(
    val inputs: List<String>,
    val virtualInputs: List<VirtualInput>,
    val inputExclusions: List<InputExclusion>,
    val environmentVariable: Map<String, String>,
    val symlinkBehavior: SymlinkBehaviorType,
)