package com.tagantroy.merkletree.cache

import com.tagantroy.merkletree.cache.SimpleCache
import com.tagantroy.merkletree.types.emptyDigest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class SimpleCacheTest {
    @Test
    fun cacheHitsAndMissesAre0ByDefault() {
        val cache = SimpleCache()
        assertEquals(0, cache.getCacheHits())
        assertEquals(0, cache.getCacheMisses())
    }

    @Test
    fun deleteNotExistingElementShouldNotCrash() {
        val cache = SimpleCache()
        cache.delete(Paths.get("."))
    }

    @Test
    fun getForAlreadyPresentElementShouldReturnIt() {
        val cache = SimpleCache()
        val path = Paths.get(".")
        val metadata = com.tagantroy.merkletree.types.metadata.Metadata(
            emptyDigest(),
            false,
            false,
            false,
            false,
            null
        )
        cache.update(path, metadata)
        assertEquals(metadata, cache.get(path))
    }

    @Test
    @DisplayName("Cache hit counter should increase if entity already exist in cache")
    fun checkHitCounter() {
        val cache = SimpleCache()
        val path = Paths.get(".")
        val metadata = com.tagantroy.merkletree.types.metadata.Metadata(
            emptyDigest(),
            false,
            false,
            false,
            false,
            null
        )
        assertEquals(0, cache.getCacheHits())
        cache.update(path, metadata)
        cache.get(path)
        assertEquals(1, cache.getCacheHits())
        assertEquals(0, cache.getCacheMisses())
    }

    @Test
    @DisplayName("Check metadata returned from Cache.get()")
    fun getForNonExistingPathShouldCreateValidMetadata() {
        val cache = SimpleCache()
        val path = Paths.get("src/test/resources/testfile.json")
        val metadata = cache.get(path)
        assertFalse(metadata.isExecutable)
        assertFalse(metadata.isDirectory)
        assertFalse(metadata.isSymlink)
        assertTrue(metadata.isRegularFile)
        assertEquals(67, metadata.digest.size)
        assertEquals("a6eb953f1fd50dbf77f4ff4716d0703a4442b840500888f4c0a550e284d8ec5b", metadata.digest.hash)
    }
}