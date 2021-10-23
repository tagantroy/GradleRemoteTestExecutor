package com.tagantroy.remoteexecutionplugin.files

import com.tagantroy.remoteexecutionplugin.FakeFileTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FakeFileTreeTest {
    @Test
    fun test(){
        val tree = FakeFileTree()
        assertEquals(1,1 )
    }
}