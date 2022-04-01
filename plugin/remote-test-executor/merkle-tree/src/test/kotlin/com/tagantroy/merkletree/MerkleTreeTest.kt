package com.tagantroy.merkletree

import com.tagantroy.merkletree.cache.SimpleCache
import com.tagantroy.types.InputSpec
import com.tagantroy.types.SymlinkBehaviorType
import org.junit.jupiter.api.Test

class MerkleTreeTest {
    @Test
    fun e2e() {
        val inputSpec = InputSpec(
            inputs = listOf(
                "merkle-tree/src/test/resources/testproj/"
            ),
            virtualInputs = listOf(),
            inputExclusions = listOf(),
            environmentVariable = mapOf("MY_PRIVATE_ENV_VAR" to "TOP_SECRET"),
            symlinkBehavior = SymlinkBehaviorType.ResolveSymlink
        );

        val simpleCache = SimpleCache()
        val execRoot = "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor"
        val workingDir = "./"
        val remoteWorkingDir = "re/"
        val merkleTree = MerkleTree(inputSpec, simpleCache)
        merkleTree.computeMerkleTree(execRoot, workingDir, remoteWorkingDir)
    }
}