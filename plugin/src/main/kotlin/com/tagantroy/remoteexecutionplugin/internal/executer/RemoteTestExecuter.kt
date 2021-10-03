package com.tagantroy.remoteexecutionplugin.internal.executer

import com.google.common.collect.ImmutableSet
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.*
import org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor
import org.gradle.api.logging.Logging
import org.gradle.internal.Factory
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseRegistry
import org.gradle.process.internal.worker.WorkerProcessFactory
import java.io.File

class RemoteTestExecuter(
    val remoteExecutionService: RemoteExecutionService,
    val workerFactory: WorkerProcessFactory, val actorFactory: ActorFactory, val moduleRegistry: ModuleRegistry,
    val workerLeaseRegistry: WorkerLeaseRegistry, val maxWorkerCount: Int,
    val clock: Clock, val documentationRegistry: DocumentationRegistry, val testFilter: DefaultTestFilter
) : TestExecuter<JvmTestExecutionSpec> {

    private val LOGGER = Logging.getLogger(RemoteTestExecuter::class.java)

    override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        val testFramework = testExecutionSpec.testFramework
        val testInstanceFactory = testFramework.processorFactory

        val currentWorkerLease: WorkerLeaseRegistry.WorkerLease = workerLeaseRegistry.getCurrentWorkerLease()

        val classpath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.classpath)
        val modulePath: Set<File> = ImmutableSet.copyOf(testExecutionSpec.modulePath)
        val testWorkerImplementationModules = testFramework.testWorkerImplementationModules

        val forkingProcessorFactory: Factory<TestClassProcessor> =
            Factory {
                ForkingTestClassProcessor(
                    currentWorkerLease,
                    workerFactory,
                    testInstanceFactory,
                    testExecutionSpec.javaForkOptions,
                    classpath,
                    modulePath,
                    testWorkerImplementationModules,
                    testFramework.workerConfigurationAction,
                    moduleRegistry,
                    documentationRegistry
                )
            }

        val processor = PatternMatchTestClassProcessor(
            testFilter,
            RunPreviousFailedFirstTestClassProcessor(
                testExecutionSpec.previousFailedTestClasses,
                MaxNParallelTestClassProcessor(
                    getMaxParallelForks(testExecutionSpec),
                    forkingProcessorFactory,
                    actorFactory
                )
            )
        )

        val testClassFiles = testExecutionSpec.candidateClassFiles

        val detector = if (testExecutionSpec.isScanForTestClasses && testFramework.detector != null) {
            val testFrameworkDetector = testFramework.detector
            testFrameworkDetector.setTestClasses(testExecutionSpec.testClassesDirs.files)
            testFrameworkDetector.setTestClasspath(classpath)
            DefaultTestClassScanner(testClassFiles, testFrameworkDetector, processor)
        } else {
            DefaultTestClassScanner(testClassFiles, null, processor)
        }

        val reProcessor = RETestClassProcessor(remoteExecutionService, testExecutionSpec)

        TestMainAction(
            detector,
            reProcessor,
            testResultProcessor,
            clock,
            testExecutionSpec.path,
            "Gradle Test Run " + testExecutionSpec.identityPath
        ).run()
    }

    override fun stopNow() {
        //TODO: stop processor here
        TODO("Not yet implemented")
    }

    private fun getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int {
        var maxParallelForks = testExecutionSpec.maxParallelForks
        if (maxParallelForks > maxWorkerCount) {
            LOGGER.info(
                "{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}",
                testExecutionSpec.path,
                maxParallelForks,
                maxWorkerCount,
                maxWorkerCount
            )
            maxParallelForks = maxWorkerCount
        }
        return maxParallelForks
    }
}