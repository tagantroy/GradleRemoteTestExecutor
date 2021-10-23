package com.tagantroy.remoteexecutionplugin

import build.bazel.remote.execution.v2.*
import com.tagantroy.remoteexecutionplugin.internal.executer.isolation.classlevel.RETestClassProcessor
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.log

data class REPath(val relativePath: Path, val absolutePath: Path)


class Node(val name: String) {
    val nodes = mutableMapOf<String, Node>()

    fun getOrCreate(name: String): Node {
        return if (!nodes.containsKey(name)) {
            val newNode = Node(name)
            nodes[name] = newNode
            newNode
        } else {
            nodes[name]!!
        }
    }

    var file = false
    var path: Path? = null
}


class FakeFileTree() {
    val root = Node("")
    fun insert(rePath: REPath) {
        val fakePath = rePath.relativePath
        val iter = fakePath.toString().split("/").iterator()
        var curNode = root
        while (iter.hasNext()) {
            val curValue = iter.next()
            curNode = curNode.getOrCreate(curValue)
        }
        curNode.file = true
        curNode.path = rePath.absolutePath
    }
}

private val logger = Logging.getLogger(FileManager::class.java)

fun relativePath(file: File, rootProjectDir: File, gradleUserHomeDir: File): Path {
    return when {
        file.absolutePath.startsWith(rootProjectDir.absolutePath) -> {
            rootProjectDir.toPath().relativize(file.toPath())
        }
        file.absolutePath.startsWith(gradleUserHomeDir.path) -> {
            gradleUserHomeDir.toPath().relativize(file.toPath())
        }
        else -> {
            logger.error("Cannot relativize")
            file.toPath()
        }
    }
}

class FileManager(
    private val rootProjectDir: File,
    private val buildDir: File,
    private val gradleUserHomeDir: File,
    private val input: Set<File>
) {
    private val logger = Logging.getLogger(FileManager::class.java)

    fun relativePathsFromVirtualRoot(): List<Path> {
        return input.map { relativePath(it, rootProjectDir, gradleUserHomeDir) }
    }

    fun buildFakeFileTree(): FakeFileTree {
        val tree = FakeFileTree()
        input.forEach {
            if (it.isFile) {
                tree.insert(REPath(relativePath(it, rootProjectDir, gradleUserHomeDir), it.absoluteFile.toPath()))
            } else if (it.isDirectory) {
                it.walk().forEach {
                    if (it.isFile) {
                        tree.insert(
                            REPath(
                                relativePath(it, rootProjectDir, gradleUserHomeDir),
                                it.absoluteFile.toPath()
                            )
                        )
                    }
                }
            }
        }
        return tree
    }


    fun build() {
        val directory = Directory.newBuilder()
            .addAllDirectories(listOf<DirectoryNode>())
            .addAllFiles(listOf())
            .build()

        input.forEach {
            logger.error("path = " + it)
//            if (Files.isDirectory(it)) {
//                it.toFile().walk().forEach {
//                    if(it.isFile){
//
//                    }else if(it.isDirectory){
//
//                    }
//                }
//
//            } else if (Files.isRegularFile(it)) {
//                FileNode
//                DirectoryNode
//
//            }
        }
    }

    private fun visitDirectory() {

    }

    fun buildMerkleTree() {
        val directoryNode =
            DirectoryNode.newBuilder().setName("asdf").setDigest(Digest.newBuilder().setHash("").build()).build()
        val fileNode = FileNode.newBuilder()
            .setDigest(Digest.newBuilder().setHash("asdf").build())
            .setIsExecutable(true)
            .setName("asdf")
            .build()
        val directory = Directory.newBuilder().addDirectories(directoryNode).build()
    }
}