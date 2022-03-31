package com.tagantroy.merkletree.cache

import com.tagantroy.merkletree.hash.toDigest
import com.tagantroy.merkletree.types.Digest
import com.tagantroy.merkletree.types.metadata.Metadata
import com.tagantroy.merkletree.types.metadata.SymlinkMetadata
import java.nio.file.Files
import java.nio.file.Path

interface Cache {
    fun get(path: Path): Metadata
    fun delete(filename: Path)
    fun update(path: Path, cacheEntry: Metadata)
    fun getCacheHits(): Long
    fun getCacheMisses(): Long
}

class SimpleCache : Cache {
    private var cacheHits: Long = 0L
    private var cacheMisses: Long = 0L
    private val map = HashMap<Path, Metadata>()

    override fun get(path: Path): Metadata {
        if (map.containsKey(path)) {
            cacheHits++
        }
        return map.getOrPut(path) {
            cacheMisses++
            computeMetadata(path)
        }
    }

    override fun delete(filename: Path) {
        map.remove(filename)
    }

    override fun update(path: Path, cacheEntry: Metadata) {
        map[path] = cacheEntry
    }

    override fun getCacheHits(): Long {
        return cacheHits
    }

    override fun getCacheMisses(): Long {
        return cacheMisses
    }

    private fun computeMetadata(path: Path): Metadata {
        val digest = path.toDigest();
        val isExecutable = Files.isExecutable(path)
        val isDirectory = Files.isDirectory(path)
        val symlinkMetadata = if (Files.isSymbolicLink(path)) {
            val target = Files.readSymbolicLink(path)
            SymlinkMetadata(target.toString(), false)
        } else null
        return Metadata(digest, isExecutable, isDirectory, symlinkMetadata)
    }
}

