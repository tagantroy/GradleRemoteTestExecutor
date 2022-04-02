package com.tagantroy.test

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FailingTest {
    @Test
    internal fun failingTest() {
        assertTrue(false)
    }
}