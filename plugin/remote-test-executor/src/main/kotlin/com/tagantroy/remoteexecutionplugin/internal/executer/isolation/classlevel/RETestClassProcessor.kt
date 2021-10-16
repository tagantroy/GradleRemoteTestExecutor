package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel

import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.gradle.api.internal.tasks.testing.TestResultProcessor

class RETestClassProcessor(val service: RemoteExecutionService, val testExecutionSpec: JvmTestExecutionSpec) : TestClassProcessor {

    override fun startProcessing(resultProcessor: TestResultProcessor) {
        // Initialize connection and etc

    }

    override fun processTestClass(testClass: TestClassRunInfo) {
        // Start execution
        val classpath = testExecutionSpec.classpath
        val framework = testExecutionSpec.testFramework
        val modulePath = testExecutionSpec.modulePath
        val candidateClassFiles = testExecutionSpec.candidateClassFiles
        val path = testExecutionSpec.path
        val identityPath = testExecutionSpec.identityPath
        val forkEvery = testExecutionSpec.forkEvery
        val javaForkOptions = testExecutionSpec.javaForkOptions
        println("classpath: $classpath")
        println("framework: ${framework.options}")
        println("modulePath: $modulePath")
        println("candidateClassFiles: $candidateClassFiles")
        println("path: $path")
        println("identityPath: $identityPath")
        println("forkEvery: $forkEvery")
        println("javaForkOptions: $javaForkOptions")
        println("classpath: $classpath")

        val inputs = classpath


    }

    override fun stop() {
    }


    override fun stopNow() {
        throw UnsupportedOperationException("stopNow() should not be invoked on remote worker TestClassProcessor")
    }
}