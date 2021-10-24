package com.tagantroy.remoteexecutionplugin.config

import build.bazel.remote.execution.v2.Platform
import java.util.*

data class Config(val doNotCache: Boolean, val instance: String, val timeoutSeconds: Long, val platform: PlatformConfig)

data class PlatformConfig(val config: TreeMap<String, String>) {
    fun toProperties(): List<Platform.Property> {
        return config.map {
            Platform.Property.newBuilder()
                .setName(it.key)
                .setValue(it.value)
                .build()
        }
    }
}