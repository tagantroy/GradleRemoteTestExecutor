package com.tagantroy.remoteexecutionplugin

import com.tagantroy.remoteexecutionplugin.internal.executer.RemoteTestExecuter
import com.tagantroy.remoteexecutionplugin.service.createRemoteExecutionService
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseService
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject

class RemoteTestExecutionPlugin @Inject constructor(
    private val moduleRegistry: ModuleRegistry,
    private val workerLeaseService: WorkerLeaseService,
    val clock: Clock,
) : Plugin<Project> {
    private val logger = Logging.getLogger(RemoteTestExecutionPlugin::class.java)
    override fun apply(project: Project) {
        if (pluginAlreadyApplied(project)) {
            return
        }

        val extension = project.extensions.create("remoteTestExecutor",RemoteTestExecutionExtensions::class.java)
        project.tasks.withType(Test::class.java).configureEach {
            configureTestTask(it, extension, project)
        }
    }

    private fun configureTestTask(
        task: Test,
        extension: RemoteTestExecutionExtensions,
    project: Project
    ) {
        val projectRootDir = project.rootProject.rootDir
        val buildDir = project.buildDir
        val gradleUserHomeDir = project.gradle.gradleUserHomeDir
        task.doFirst(
            ConditionalTaskAction(
                { extension.enabled.get() },
                InitTaskAction(extension, moduleRegistry, workerLeaseService,  clock, projectRootDir, buildDir, gradleUserHomeDir)
            )
        )
    }

    private fun pluginAlreadyApplied(project: Project): Boolean {
        return project.plugins.any { it.javaClass.name.equals(RemoteTestExecutionPlugin::class.java.name) }
    }
}

class ConditionalTaskAction(val predicate: (Task) -> Boolean, val action: Action<Task>) : Action<Task> {
    override fun execute(t: Task) {
        if (predicate(t)) {
            action.execute(t)
        }
    }
}

fun createRemoteTestExecuter(
    extensions: RemoteTestExecutionExtensions,
    moduleRegistry: ModuleRegistry,
    workerLeaseService: WorkerLeaseService,
    clock: Clock,
    testFilter: TestFilter,
    projectRoot: File,
    buildDir: File,
    gradleUserHomeDir: File,
): RemoteTestExecuter {
    val host = extensions.host.get()
    val service = createRemoteExecutionService(host)
    return RemoteTestExecuter(service, moduleRegistry, workerLeaseService, clock, testFilter, projectRoot, buildDir, gradleUserHomeDir)
}

class InitTaskAction(
    private val extension: RemoteTestExecutionExtensions,
    private val moduleRegistry: ModuleRegistry,
    private val workerLeaseService: WorkerLeaseService,
    private val clock: Clock,
    private val projectRoot: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
) :
    Action<Task> {
    override fun execute(task: Task) {
        val testTask = task as Test
        setTestExecuter(
            task,
            createRemoteTestExecuter(extension, moduleRegistry, workerLeaseService, clock, testTask.filter, projectRoot, buildDir, gradleUserHomeDir)
        )
    }
}

fun setTestExecuter(task: Task, executer: RemoteTestExecuter) {
    invoke<Void>(
        declaredMethod(Test::class.java, "setTestExecuter", TestExecuter::class.java)!!,
        task, executer
    )
}

private fun declaredMethod(type: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method? {
    return try {
        makeAccessible(type.getDeclaredMethod(methodName, *paramTypes))
    } catch (e: NoSuchMethodException) {
        throw RuntimeException(e)
    }
}

private fun makeAccessible(method: Method): Method? {
    method.isAccessible = true
    return method
}

private fun <T> invoke(method: Method, instance: Any, vararg args: Any): T {
    return try {
        val result = method.invoke(instance, *args)
        result as T
    } catch (e: IllegalAccessException) {
        throw RuntimeException(e)
    } catch (e: InvocationTargetException) {
        throw RuntimeException(e)
    }
}