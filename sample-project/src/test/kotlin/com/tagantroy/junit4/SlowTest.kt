package com.tagantroy.junit4

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlowTest {
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