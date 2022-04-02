package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel

import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecution.Client
import com.tagantroy.types.*
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import java.io.File
import java.time.Duration
import java.util.*

class ModuleLevelIsolationAction(
    private val remoteExecutionClient: Client,
    private val testExecutionSpec: JvmTestExecutionSpec,
    private val testResultProcessor: TestResultProcessor,
    private val testFilter: TestFilter,
    private val moduleRegistry: ModuleRegistry,
    private val clock: Clock,
    private val rootProjectDir: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
) : Runnable {

    private val logger = Logging.getLogger(ModuleLevelIsolationAction::class.java)

    override fun run() {
        val testFramework = testExecutionSpec.testFramework

        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath.filter { it.exists() })
        val modulePath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.modulePath)
        val testWorkerImplementationModules = testFramework.testWorkerImplementationModules
        val additionalClassPath = moduleRegistry.additionalClassPath.asFiles.filter { it.exists() }

        println("classpath = $classpath")
        println("modulePath = $modulePath")
        println("gradleUserHomeDir = $gradleUserHomeDir")
        println("path = ${testExecutionSpec.path}")
        println("identityPath = ${testExecutionSpec.identityPath}")
        println("testWorkerImplementationModules = $testWorkerImplementationModules")
        println("additionalClassPath = $additionalClassPath")
        println("excludePatterns = ${testFilter.excludePatterns}")
        println("includePatterns = ${testFilter.includePatterns}")
        println("isFailOnNoMatchingTests = ${testFilter.isFailOnNoMatchingTests}")

        val input = classpath.filter { it.startsWith(rootProjectDir) }.map { it.relativeTo(rootProjectDir) }
            .map { it.toPath().toString() }
        val gradleUserHomeArtifacts = classpath.filterNot { it.startsWith(rootProjectDir) }.map {
            VirtualInput(
                it.relativeTo(gradleUserHomeDir).toString(), it.readBytes(), false, false
            )
        }

        val preparedClassPath = input + gradleUserHomeArtifacts.map { it.path }

        val arguments = listOf(
            "java",
            "-jar",
            "junit-platform-console-standalone-1.8.1.jar",
            "-cp",
            preparedClassPath.joinToString(":"),
            "--scan-class-path",
            input.joinToString(":"),
            "--reports-dir",
            "./report"
        )

        val junitPlatformConsole =
            File("sample-project/junit-platform-console-standalone-1.8.1.jar")
        val junitPlatformConsoleJar = VirtualInput(
            "junit-platform-console-standalone-1.8.1.jar",
            junitPlatformConsole.readBytes(),
            isExecutable = false,
            isEmptyDirectory = false
        )

        val command = Command(
            identifiers = Identifiers(
                commandId = "Test Run ${testExecutionSpec.identityPath}",
                invocationId = UUID.randomUUID().toString(),
                correlatedInvocationId = "",
                toolName = "Gradle Remote Execution Plugin",
                toolVersion = "0.0.1",
                executionId = "",
            ),
            args = arguments,
            execRoot = rootProjectDir.path,
            workingDir = ".",
            remoteWorkingDir = ".",
            inputSpec = InputSpec(
                inputs = input,
                virtualInputs = gradleUserHomeArtifacts + junitPlatformConsoleJar,
                inputExclusions = emptyList(),
                environmentVariable = mapOf(),
                symlinkBehavior = SymlinkBehaviorType.ResolveSymlink
            ),
            outputFiles = emptyList(),
            outputDirs = listOf("./report"),
            timeout = Duration.ofSeconds(100),
            platform = mapOf("OSFamily" to "Linux", "platform" to "java8")
        )
        val executionOptions =
            ExecutionOptions(acceptCached = false, doNotCache = true, downloadOutputs = false, downloadOutErr = false)

        remoteExecutionClient.execute(command, executionOptions)
    }
}

