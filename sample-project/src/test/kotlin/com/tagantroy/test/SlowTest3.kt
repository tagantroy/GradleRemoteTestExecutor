package com.tagantroy.test

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlowTest3 {
    @Test
    fun test1(){
        Thread.sleep(1_000)
        assertTrue(true)
    }

    @Test
    fun test2(){
        Thread.sleep(3_000)
        assertFalse(false)
    }

    @Test
    fun test3(){
        Thread.sleep(1_000)
        assertTrue(true)
    }

    @Test
    fun test4(){
        Thread.sleep(3_000)
        assertFalse(false)
    }

    @Test
    fun test5(){
        Thread.sleep(1_000)
        assertTrue(true)
    }
}