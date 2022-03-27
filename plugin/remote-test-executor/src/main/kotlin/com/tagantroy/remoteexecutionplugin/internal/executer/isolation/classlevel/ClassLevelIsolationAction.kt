package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel

import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
import org.gradle.api.internal.tasks.testing.processors.TestMainAction
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import java.io.File


class ClassLevelIsolationAction(
    private val remoteExecutionService: RemoteExecutionService,
    private val testExecutionSpec: JvmTestExecutionSpec,
    private val testResultProcessor: TestResultProcessor,
    private val workerLeaseService: WorkerLeaseService,
    private val testFilter: TestFilter,
    private val moduleRegistry: ModuleRegistry,
    private val clock: Clock,
    private val rootProjectDir: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
) : Runnable {
    private val logger = Logging.getLogger(ClassLevelIsolationAction::class.java)
    override fun run() {
        val testFramework = testExecutionSpec.testFramework

        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath)
//        val modulePath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.modulePath)
//        val testWorkerImplementationModules = testFramework.testWorkerImplementationModules
//        val additionalClassPath = moduleRegistry.additionalClassPath.asFiles

//        testFilter.excludePatterns
//        testFilter.includePatterns
//        testFilter.isFailOnNoMatchingTests

        val testClassFiles = testExecutionSpec.candidateClassFiles
        val processor =
            RETestClassProcessor(remoteExecutionService, testExecutionSpec, rootProjectDir, buildDir, gradleUserHomeDir)
        val detector = if (testExecutionSpec.isScanForTestClasses && testFramework.detector != null) {
            val testFrameworkDetector = testFramework.detector
            testFrameworkDetector.setTestClasses(testExecutionSpec.testClassesDirs.files)
            testFrameworkDetector.setTestClasspath(classpath)
            DefaultTestClassScanner(testClassFiles, testFrameworkDetector, processor)
        } else {
            DefaultTestClassScanner(testClassFiles, null, processor)
        }
        logger.info("Run TestMainAction with RETestClassProcessor")
        TestMainAction(
            detector,
            processor,
            testResultProcessor,
            workerLeaseService,
            clock,
            testExecutionSpec.path,
            "Gradle Test Run " + testExecutionSpec.identityPath
        )
    }
}