package com.tagantroy.remoteexecutionplugin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RemoteExecutionServiceTest {
    val config = Config("localhost:8980")
    val service = createRemoteExecutionService(config)

    @Test
    fun testUpload(){
        service.execute()
    }
}