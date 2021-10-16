package com.tagantroy.test

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrokenTest {
    @Test
    fun test(){
        println("stdout message")
        System.err.println("error message")
        assertTrue(true)
    }
}