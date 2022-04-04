package com.tagantroy.remoteexecutionplugin.internal.executer

import com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel.ClassLevelIsolationAction
import com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel.ModuleLevelIsolationAction
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import java.io.File
import java.util.*

class RemoteTestExecuter(
    private val remoteExecutionService:  com.tagantroy.remoteexecution.Client,
    private val moduleRegistry: ModuleRegistry,
    private val workerLeaseService: WorkerLeaseService,
    private val clock: Clock,
    private val testFilter: TestFilter,
    private val rootProjectDir: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
    private val invocationId: String,
) : TestExecuter<JvmTestExecutionSpec> {

    private val logger = Logging.getLogger(RemoteTestExecuter::class.java)

    override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        if(classLevelIsolation()) {
            logger.info("Execute with class level isolation")
            ClassLevelIsolationAction(remoteExecutionService, testExecutionSpec, testResultProcessor, workerLeaseService, testFilter, moduleRegistry, clock, rootProjectDir, buildDir, gradleUserHomeDir).run()
        } else {
            logger.info("Execute with module level isolation")
            ModuleLevelIsolationAction(remoteExecutionService, testExecutionSpec, testFilter, moduleRegistry, rootProjectDir, gradleUserHomeDir, buildDir, invocationId).run()
        }
    }

    private fun classLevelIsolation() : Boolean {
        return false
    }

    override fun stopNow() {
        TODO("Not yet implemented")
    }
}