package com.tagantroy.remoteexecutionplugin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RemoteExecutionServiceTest {
    val host = "localhost:8980"
    val service = createRemoteExecutionService(host)

    @Test
    fun testUpload(){
        service.execute()
    }
}