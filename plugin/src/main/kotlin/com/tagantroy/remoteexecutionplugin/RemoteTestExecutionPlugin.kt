package com.tagantroy.remoteexecutionplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class RemoteTestExecutionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extensions = target.extensions.create<RemoteTestExecutionExtensions>("test_remote_execution")
        TODO("Not yet implemented")
    }
}