package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel

import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecution.Client
import com.tagantroy.types.*
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import java.io.File
import java.time.Duration
import java.util.*

class ModuleLevelIsolationAction(
    private val remoteExecutionClient: Client,
    private val testExecutionSpec: JvmTestExecutionSpec,
    private val testFilter: TestFilter,
    private val moduleRegistry: ModuleRegistry,
    private val rootProjectDir: File,
    private val gradleUserHomeDir: File,
    private val buildDir: File,
    private val invocationId: String,
) : Runnable {

    private val logger = Logging.getLogger(ModuleLevelIsolationAction::class.java)

    override fun run() {
        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath.filter { it.exists() })
        val additionalClassPath = moduleRegistry.additionalClassPath.asFiles.filter { it.exists() }

        if (testFilter.excludePatterns.isNotEmpty() || testFilter.includePatterns.isNotEmpty()) {
            logger.warn("TestFilters are not supported yet")
        }
        val localBuildArtifacts = (classpath + additionalClassPath).filter { it.startsWith(rootProjectDir) }
            .map { it.relativeTo(rootProjectDir) }
            .map { it.toPath().toString() }
        val gradleUserHomeArtifacts =
            (classpath + additionalClassPath).filterNot { it.startsWith(rootProjectDir) }.map {
                VirtualInput(
                    it.relativeTo(gradleUserHomeDir).toString(), it.readBytes(), false, false
                )
            }
        val preparedClassPath = localBuildArtifacts + gradleUserHomeArtifacts.map { it.path }

        val arguments = listOf(
            "java",
            "-jar",
            "junit-platform-console-standalone-1.8.2.jar",
            "-cp",
            preparedClassPath.joinToString(":"),
            "--scan-class-path",
            localBuildArtifacts.joinToString(":"),
            "--reports-dir",
            "./report"
        )

        val junitPlatformConsole =
            File("sample-project/junit-platform-console-standalone-1.8.2.jar")
        val junitPlatformConsoleJar = VirtualInput(
            "junit-platform-console-standalone-1.8.2.jar",
            junitPlatformConsole.readBytes(),
            isExecutable = false,
            isEmptyDirectory = false
        )

        val command = Command(
            identifiers = Identifiers(
                commandId = "Test Run ${testExecutionSpec.identityPath}",
                invocationId = invocationId,
                correlatedInvocationId = "",
                toolName = "Gradle Remote Execution Plugin",
                toolVersion = "0.0.1",
                executionId = UUID.randomUUID().toString(),
            ),
            args = arguments,
            execRoot = rootProjectDir.path,
            workingDir = ".",
            remoteWorkingDir = ".",
            inputSpec = InputSpec(
                inputs = localBuildArtifacts,
                virtualInputs = gradleUserHomeArtifacts + junitPlatformConsoleJar,
                inputExclusions = emptyList(),
                environmentVariable = mapOf(),
                symlinkBehavior = SymlinkBehaviorType.ResolveSymlink
            ),
            outputFiles = emptyList(),
            outputDirs = listOf("./report/"),
            timeout = Duration.ofSeconds(100),
            platform = mapOf("OSFamily" to "Linux", "platform" to "java8")
        )
        val executionOptions =
            ExecutionOptions(acceptCached = false, doNotCache = true, downloadOutputs = false, downloadOutErr = false)

        val result = remoteExecutionClient.execute(command, executionOptions)
        val stdout = remoteExecutionClient.downloadFile(result.result.stdoutDigest).decodeToString()
        val stderr = remoteExecutionClient.downloadFile(result.result.stderrDigest).decodeToString()
        println(stdout.removePrefix("stdout message"))
        System.err.println(stderr.removePrefix("error message"))

        val reportsDir = File(buildDir, "test-results")
        reportsDir.mkdir()
        val testReports = File(reportsDir, "remote-execution-test-reports")
        testReports.delete()
        testReports.mkdir()
        remoteExecutionClient.downloadOutputDirectory(
            result.result.outputDirectoriesList.first(),
            testReports.toPath()
        )

        if (result.result.exitCode != 0) {
            throw RuntimeException("Exit code: ${result.result.exitCode}")
        }
    }
}

