package com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel

import com.tagantroy.remoteexecution.Client
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logging
import java.io.File

class RETestClassProcessor(
    val service: Client,
    val testExecutionSpec: JvmTestExecutionSpec,
    val rootProjectDir: File,
    val buildDir: File,
    val gradleUserHomeDir: File,
) : TestClassProcessor {
    private val logger = Logging.getLogger(RETestClassProcessor::class.java)
    override fun startProcessing(resultProcessor: TestResultProcessor) {
        // Initialize connection and etc

    }

    override fun processTestClass(testClass: TestClassRunInfo) {

        // Start execution
        val classpath = testExecutionSpec.classpath
//        val framework = testExecutionSpec.testFramework
//        val modulePath = testExecutionSpec.modulePath
//        val candidateClassFiles = testExecutionSpec.candidateClassFiles
//        val path = testExecutionSpec.path
//        val identityPath = testExecutionSpec.identityPath
//        val forkEvery = testExecutionSpec.forkEvery
//        val javaForkOptions = testExecutionSpec.javaForkOptions
//        println("classpath: $classpath")
//        println("framework: ${framework.options}")
//        println("modulePath: $modulePath")
//        println("candidateClassFiles: $candidateClassFiles")
//        println("path: $path")
//        println("identityPath: $identityPath")
//        println("forkEvery: $forkEvery")
//        println("javaForkOptions: $javaForkOptions")
//        println("classpath: $classpath")
        val junitPlatformConsole = File("$rootProjectDir/junit-platform-console-standalone-1.8.1.jar").toPath()
        val fixedClasspath = classpath.map {
            if (it.absolutePath.startsWith(rootProjectDir.absolutePath)) {
                rootProjectDir.toPath().relativize(it.toPath())
            } else if (it.absolutePath.startsWith(gradleUserHomeDir.path)) {
                gradleUserHomeDir.toPath().relativize(it.toPath())
            } else {
                logger.error("Cannot relativize: $it")
                it.toPath()
            }
        }

        val mergedClasspath = fixedClasspath.joinToString(":")
        val arguments = listOf(
            "java",
            "-jar",
            "junit-platform-console-standalone-1.8.1.jar",
            "-cp",
            mergedClasspath,
            "--scan-classpath",
            "--reports-dir",
            "./report",
            "--select-class=${testClass.testClassName}"
        )
        logger.info("Execute remote action")
//        service.execute(arguments, mapOf(), ImmutableList.copyOf(fixedClasspath+junitPlatformConsole))
    }

    override fun stop() {
    }


    override fun stopNow() {
        throw UnsupportedOperationException("stopNow() should not be invoked on remote worker TestClassProcessor")
    }
}