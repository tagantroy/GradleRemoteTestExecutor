package com.tagantroy.merkletree.paths

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class GetExecRootRelPathsTest {
    @Test
    fun test() {
        val localWorkingDir = "."
        val remoteWorkingDir = "."
        val execRoot = "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor"
        val absPath = "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor/merkle-tree/src/test/resources/testproj"
        val (normPath, remoteNormPath) = getExecRootRelPaths(Paths.get(absPath), execRoot, localWorkingDir, remoteWorkingDir)
        assertEquals("merkle-tree/src/test/resources/testproj", normPath)
        assertEquals("merkle-tree/src/test/resources/testproj", remoteNormPath)
    }
}