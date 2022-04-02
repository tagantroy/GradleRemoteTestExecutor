package com.tagantroy.junit4

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class OldTests {
    @Test
    fun passedTest() {
        assertTrue(true)
    }

    @Test
    fun failedTest() {
        assertTrue(false)
    }

    @Test
    fun assumptionFailure() {
        assumeTrue(false)
    }
}