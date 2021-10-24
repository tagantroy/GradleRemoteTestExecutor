package com.tagantroy.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalculatorTest {
    private val calculator = Calculator()

    @Test
    fun testSum() {
        assertEquals(7, calculator.sum(2, 5))
    }

    @Test
    fun testMultiply() {
        assertEquals(10, calculator.multiply(2, 5))
    }
}