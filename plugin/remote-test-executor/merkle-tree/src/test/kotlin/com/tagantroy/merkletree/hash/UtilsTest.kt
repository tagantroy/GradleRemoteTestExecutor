package com.tagantroy.merkletree.hash

import com.tagantroy.merkletree.hash.toDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class UtilsTest {
    @Test
    internal fun fromPath() {
        val path = Paths.get("src/test/resources/testfile.json")
        val digest = path.toDigest()
        assertEquals(67, digest.size)
        assertEquals("a6eb953f1fd50dbf77f4ff4716d0703a4442b840500888f4c0a550e284d8ec5b", digest.hash)
    }
}