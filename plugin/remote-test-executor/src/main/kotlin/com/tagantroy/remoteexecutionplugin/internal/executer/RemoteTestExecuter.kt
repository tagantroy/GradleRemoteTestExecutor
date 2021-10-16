package com.tagantroy.remoteexecutionplugin.internal.executer

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

class RemoteTestExecuter(
    private val remoteExecutionService: RemoteExecutionService,
    private val moduleRegistry: ModuleRegistry,
    val clock: Clock,
    private val testFilter: TestFilter
) : TestExecuter<JvmTestExecutionSpec> {

    private val LOGGER = Logging.getLogger(RemoteTestExecuter::class.java)

    override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        if(classLevelIsolation()) {
            LOGGER.info("Execute with class level isolation")
            ClassLevelIsolationAction(remoteExecutionService, testExecutionSpec, testResultProcessor, testFilter, moduleRegistry, clock).run()
        } else {
            LOGGER.info("Execute with module level isolation")
            ModuleLevelIsolationAction(remoteExecutionService, testExecutionSpec, testResultProcessor, testFilter, moduleRegistry, clock).run()
        }
    }

    private fun classLevelIsolation() : Boolean {
        return false
    }

    override fun stopNow() {
        TODO("Not yet implemented")
    }
}