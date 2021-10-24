package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecutionplugin.FileManager
import com.tagantroy.remoteexecutionplugin.config.Config
import com.tagantroy.remoteexecutionplugin.config.PlatformConfig
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import java.io.File
import java.util.*

class ModuleLevelIsolationAction(
    private val remoteExecutionService: RemoteExecutionService,
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

        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath)
//        val modulePath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.modulePath)
//        val testWorkerImplementationModules = testFramework.testWorkerImplementationModules
//        val additionalClassPath = moduleRegistry.additionalClassPath.asFiles

        logger.debug("classpath = $classpath")
//        logger.debug("modulePath = $modulePath")
//        logger.debug("testWorkerImplementationModules = $testWorkerImplementationModules")
//        logger.debug("additionalClassPath = $additionalClassPath")
//        logger.debug("excludePatterns = ${testFilter.excludePatterns}")
//        logger.debug("includePatterns = ${testFilter.includePatterns}")
//        logger.debug("isFailOnNoMatchingTests = ${testFilter.isFailOnNoMatchingTests}")

        // TODO add support for filters
        val fileManager = FileManager(rootProjectDir, buildDir, gradleUserHomeDir, classpath + setOf(File("/home/ivanbalaksha/work/GradleRemoteTestExecutor/sample-project/junit-platform-console-standalone-1.8.1.jar")), remoteExecutionService)
        val fixedClasspath = fileManager.relativePathsFromVirtualRoot()
        val mergedClasspath = fixedClasspath.joinToString(":")
        val junitPlatformConsole = File("sample-project/junit-platform-console-standalone-1.8.1.jar").toPath().toString()
        val arguments = listOf(
            "java",
            "-jar",
            junitPlatformConsole,
            "-cp",
            mergedClasspath,
            "--scan-classpath",
            "--reports-dir",
            "./report"
        )
        logger.info("Execute remote action")
        val treeMap = TreeMap<String, String>()
        treeMap["OSFamily"]="Linux"
        treeMap["container-image"]="docker://hub.docker.com/openjdk/8u312-slim@sha256:299d070740d9d1c12c80502dd8a8a05c67e85290da1674e7192ba1a873bd2f89"
        val config = Config(false, "remote-execution", 600, PlatformConfig(treeMap))
        remoteExecutionService.execute(arguments, mapOf(), fileManager.upload(), config)
    }
}

