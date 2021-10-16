package com.tagantroy.remoteexecutionplugin

import com.tagantroy.remoteexecutionplugin.internal.executer.RemoteTestExecuter
import com.tagantroy.remoteexecutionplugin.service.createRemoteExecutionService
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.internal.time.Clock
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject

class RemoteTestExecutionPlugin @Inject constructor(
    val objectFactory: ObjectFactory,
    val providerFactory: ProviderFactory,
    val moduleRegistry: ModuleRegistry,
    val clock: Clock,
) : Plugin<Project> {
    override fun apply(project: Project) {
        if (pluginAlreadyApplied(project)) {
            return
        }
        val extension = project.extensions.create("remoteTestExecutor",RemoteTestExecutionExtensions::class.java)
        project.tasks.withType(Test::class.java).configureEach {
            configureTestTask(it, objectFactory, providerFactory, extension)
        }
    }

    private fun configureTestTask(
        task: Test,
        objectFactory: ObjectFactory,
        providerFactory: ProviderFactory,
        extension: RemoteTestExecutionExtensions
    ) {
//        val gradleVersion = VersionNumber.parse(task.project.gradle.gradleVersion)
        task.doFirst(
            ConditionalTaskAction(
                { extension.enabled.get() },
                InitTaskAction(extension, moduleRegistry, clock)
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
    clock: Clock,
    testFilter: TestFilter,
): RemoteTestExecuter {
    val host = extensions.host.get()
    val service = createRemoteExecutionService(host)
    return RemoteTestExecuter(service, moduleRegistry, clock, testFilter)
}

class InitTaskAction(
    private val extension: RemoteTestExecutionExtensions,
    private val moduleRegistry: ModuleRegistry,
    private val clock: Clock,
) :
    Action<Task> {
    override fun execute(task: Task) {
        val testTask = task as Test
        setTestExecuter(
            task,
            createRemoteTestExecuter(extension, moduleRegistry, clock, testTask.filter)
        )
    }
}

class FinalizeTaskAction : Action<Task> {
    override fun execute(t: Task) {

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