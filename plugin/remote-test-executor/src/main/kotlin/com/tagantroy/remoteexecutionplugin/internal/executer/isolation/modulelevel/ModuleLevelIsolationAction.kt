package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import java.io.File

class ModuleLevelIsolationAction(
    private val remoteExecutionService: RemoteExecutionService,
    private val testExecutionSpec: JvmTestExecutionSpec,
    private val testResultProcessor: TestResultProcessor,
    private val testFilter: TestFilter,
    private val moduleRegistry: ModuleRegistry,
    private val clock: Clock
) : Runnable {
    private val logger = Logging.getLogger(ModuleLevelIsolationAction::class.java)
    override fun run() {
        val testFramework = testExecutionSpec.testFramework

        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath)
        val modulePath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.modulePath)
        val testWorkerImplementationModules = testFramework.testWorkerImplementationModules
        val additionalClassPath = moduleRegistry.additionalClassPath.asFiles

        logger.debug("classpath = $classpath")
        logger.debug("modulePath = $modulePath")
        logger.debug("testWorkerImplementationModules = $testWorkerImplementationModules")
        logger.debug("additionalClassPath = $additionalClassPath")
        logger.debug("excludePatterns = ${testFilter.excludePatterns}")
        logger.debug("includePatterns = ${testFilter.includePatterns}")
        logger.debug("isFailOnNoMatchingTests = ${testFilter.isFailOnNoMatchingTests}")

        // TODO add support for filters
        val mergedClasspath = classpath.joinToString(":")
        val arguments = listOf(
            "java",
            "-jar",
            "junit-platform-console-standalone-1.8.1.jar",
            "-cp",
            mergedClasspath,
            "--scan-classpath",
            "--reports-dir",
            "./report"
        )
        logger.info("Execute remote action")
        remoteExecutionService.execute(arguments, mapOf(), ImmutableList.copyOf(classpath))
    }
}

