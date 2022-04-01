package com.tagantroy.merkletree.paths

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class GetRelativePathTest {
    @Test
    fun test() {
        val base = "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor"
        val path =
            "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor/merkle-tree/src/test/resources/testproj"
        val res = getRelativePath(Paths.get(base), Paths.get(path))
        assertEquals("merkle-tree/src/test/resources/testproj", res.toString())
    }
}