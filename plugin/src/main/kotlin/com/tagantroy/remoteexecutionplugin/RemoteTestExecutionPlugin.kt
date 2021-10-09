package com.tagantroy.remoteexecutionplugin

import com.tagantroy.remoteexecutionplugin.internal.executer.RemoteTestExecuter
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import com.tagantroy.remoteexecutionplugin.service.createRemoteExecutionService
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.testing.Test
import org.gradle.util.internal.VersionNumber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject


class RemoteTestExecutionPlugin @Inject constructor(val objectFactory: ObjectFactory, val providerFactory: ProviderFactory) : Plugin<Project> {
    override fun apply(project: Project) {
        if(pluginAlreadyApplied(project)){
            return
        }
        project.tasks.withType(Test::class.java).configureEach {
            configureTestTask(it, objectFactory, providerFactory)
        }
    }

    private fun configureTestTask(task: Test, objectFactory: ObjectFactory, providerFactory: ProviderFactory) {
//        val gradleVersion = VersionNumber.parse(task.project.gradle.gradleVersion)
        val extension = objectFactory.newInstance(RemoteTestExecutionExtensions::class.java)
        task.doFirst(ConditionalTaskAction({extension.enabled.get()}, InitTaskAction()))
//        task.doLast()
    }

    private fun pluginAlreadyApplied(project: Project): Boolean {
        return project.plugins.any { it.javaClass.name.equals(RemoteTestExecutionPlugin::class.java.name) }
    }
}

class ConditionalTaskAction(val predicate: (Task)->Boolean,val action: Action<Task>) : Action<Task> {
    override fun execute(t: Task) {
        if(predicate(t)){
            action.execute(t)
        }
    }
}

fun createRemoteTestExecuter(task: Task, objectFactory: ObjectFactory, extensions: RemoteTestExecutionExtensions): RemoteTestExecuter {
    val host = extensions.host.get()
    val service = createRemoteExecutionService(host)
    return RemoteTestExecuter(service,)
}

class InitTaskAction(val objectFactory: ObjectFactory) : Action<Task>{
    override fun execute(task: Task) {
        setTestExecuter(task, createRemoteTestExecuter(task, objectFactory))
    }
}

class FinalizeTaskAction : Action<Task>{
    override fun execute(t: Task) {

    }

}

fun setTestExecuter(task: Test, executer: RemoteTestExecuter) {
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