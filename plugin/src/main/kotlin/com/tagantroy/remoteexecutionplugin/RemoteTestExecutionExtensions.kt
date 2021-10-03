package com.tagantroy.remoteexecutionplugin

import org.gradle.api.provider.Property

interface RemoteTestExecutionExtensions {
    val enabled: Property<Boolean>
}