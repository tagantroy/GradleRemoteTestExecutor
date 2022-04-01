package com.tagantroy.remoteexecution

import com.tagantroy.types.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class ClientTest {
    @Test
    fun e2e() {
        val config = com.tagantroy.remoteexecution.config.Config("localhost:8980", "localhost:8980")
        val client = Client.fromConfig(config)
        val command = Command(
            identifiers = Identifiers(
                commandId = "E2E test",
                invocationId = UUID.randomUUID().toString(),
                correlatedInvocationId = "",
                toolName = "",
                toolVersion = "",
                executionId = UUID.randomUUID().toString(),
            ),
            args = listOf("/bin/bash", "merkle-tree/src/test/resources/testproj/twofiles/script.sh"),
            execRoot = "/Users/ivanbalaksha/work/GradleRemoteTestExecutor/plugin/remote-test-executor",
            workingDir = ".",
            remoteWorkingDir = ".",
            inputSpec = InputSpec(
                inputs = listOf("./merkle-tree/src/test/resources/testproj/"),
                virtualInputs = emptyList(),
                inputExclusions = emptyList(),
                environmentVariable = emptyMap(),
                symlinkBehavior = SymlinkBehaviorType.ResolveSymlink
            ),
            outputFiles = emptyList(),
            outputDirs = emptyList(),
            timeout = Duration.ofMinutes(1),
            platform = mapOf("OSFamily" to "Linux", "platform" to "java8")
        )
        val executionOptions = ExecutionOptions(
            acceptCached = false,
            doNotCache = true,
            downloadOutputs = true,
            downloadOutErr = true
        )
        client.execute(command, executionOptions)
    }
}