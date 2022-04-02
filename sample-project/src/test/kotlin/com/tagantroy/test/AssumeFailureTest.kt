package com.tagantroy.test

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class AssumeFailureTest {
    @Test
    internal fun name() {
        assumeTrue(false)
    }
}