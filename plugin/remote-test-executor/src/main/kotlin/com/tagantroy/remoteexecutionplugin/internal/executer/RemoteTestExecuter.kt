package com.tagantroy.remoteexecutionplugin.internal.executer

import com.tagantroy.remoteexecutionplugin.FileManager
import com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel.ClassLevelIsolationAction
import com.tagantroy.remoteexecutionplugin.internal.executer.isolation.modulelevel.ModuleLevelIsolationAction
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import java.io.File

class RemoteTestExecuter(
    private val remoteExecutionService: RemoteExecutionService,
    private val moduleRegistry: ModuleRegistry,
    private val clock: Clock,
    private val testFilter: TestFilter,
    private val rootProjectDir: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
) : TestExecuter<JvmTestExecutionSpec> {

    private val logger = Logging.getLogger(RemoteTestExecuter::class.java)

    override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        if(classLevelIsolation()) {
            logger.info("Execute with class level isolation")
            ClassLevelIsolationAction(remoteExecutionService, testExecutionSpec, testResultProcessor, testFilter, moduleRegistry, clock, rootProjectDir, buildDir, gradleUserHomeDir).run()
        } else {
            logger.info("Execute with module level isolation")
            ModuleLevelIsolationAction(remoteExecutionService, testExecutionSpec, testResultProcessor, testFilter, moduleRegistry, clock, rootProjectDir, buildDir, gradleUserHomeDir).run()
        }
    }

    private fun classLevelIsolation() : Boolean {
        return false
    }

    override fun stopNow() {
        TODO("Not yet implemented")
    }
}