package com.tagantroy.remoteexecutionplugin

import build.bazel.remote.execution.v2.*
import com.google.common.hash.HashCode
import com.google.protobuf.ByteString
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import com.tagantroy.remoteexecutionplugin.service.SHA256
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveAction
import java.util.concurrent.RecursiveTask
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes
import kotlin.streams.toList

data class REPath(val relativePath: Path, val absolutePath: Path)

class Node(val name: String, val currentVirtualPath: String, val file: Boolean) {
    val nodes = mutableMapOf<String, Node>()

    fun getOrCreate(name: String, currentVirtualPath: String): Node {
        return if (!nodes.containsKey(name)) {
            val newNode = Node(name, currentVirtualPath, false)
            nodes[name] = newNode
            newNode
        } else {
            nodes[name]!!
        }
    }

    fun insert(curValue: String, newNode: Node) {
        nodes[curValue] = newNode
    }

    var path: Path? = null
    var hash: HashCode? = null
    var sizeBytes: Long? = null
}

class FakeFileTree() {
    val root = Node("root", "root", false)
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun insert(rePath: REPath) {
        val fakePath = rePath.relativePath
        val arr = fakePath.toString().split("/")
        var curNode = root
        arr.forEachIndexed { index, curValue ->
            val currentPath = arr.subList(0, index + 1).joinToString("/")
            if (index != arr.size - 1) {
                curNode = curNode.getOrCreate(curValue, currentPath)
            } else {
                val newNode = Node(curValue, currentPath, true)
                newNode.path = rePath.absolutePath
                newNode.hash = calculateHashForFile(rePath.absolutePath)
                newNode.sizeBytes = rePath.absolutePath.fileSize()
                curNode.insert(curValue, newNode)
            }
        }
    }

    private fun calculateHashForFile(path: Path): HashCode {
        return SHA256.hash(path)
    }

    fun calculateHashes() {
        reqCalculateHash(root);
    }

    private fun reqCalculateHash(node: Node) {
        if (node.file) {
            return
        } else {
            node.nodes.forEach { (_, u) ->
                reqCalculateHash(u)
            }
            node.hash =
                SHA256.hashUnordered(listOf(SHA256.hash(node.currentVirtualPath)) + node.nodes.values.mapNotNull { it.hash }
                    .toList())
        }
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
            logger.error("Cannot relativize: $file")
            file.toPath()
        }
    }
}

fun createTask(node: Node, service: RemoteExecutionService): RecursiveTask<Pair<Node, Digest>>{
    return if (node.file) {
        FileManager.UploadFileTask(node, service)
    } else {
        FileManager.UploadDirectoryTask(node, service)
    }
}

class FileManager(
    private val rootProjectDir: File,
    private val gradleUserHomeDir: File,
    private val input: Set<File>,
    private val service: RemoteExecutionService
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
        tree.calculateHashes()
        return tree
    }


    fun upload(): Digest {
        val tree = buildFakeFileTree()

        return reqUpload(tree.root)
    }

    class UploadFileTask(private val node: Node, private val service: RemoteExecutionService) : RecursiveTask<Pair<Node, Digest>>() {
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        override fun compute(): Pair<Node, Digest> {
            val dataDigest = SHA256.compute(node.hash!!, node.sizeBytes!!)
            val dataRequest = BatchUpdateBlobsRequest.Request.newBuilder()
                .setDigest(dataDigest)
                .setData(ByteString.copyFrom(node.path!!.readBytes()))
                .build()
            val fileNode = FileNode.newBuilder().setName(node.name).setDigest(
                dataDigest
            ).setIsExecutable(true).build()
            val fileNodeRequest = BatchUpdateBlobsRequest.Request.newBuilder()
                .setData(fileNode.toByteString())
                .setDigest(SHA256.compute(fileNode))
                .build()
            val updateRequest = BatchUpdateBlobsRequest.newBuilder()
                .addRequests(dataRequest)
                .addRequests(fileNodeRequest)
                .build()
            val res = service.upload(updateRequest)
            return node to res.getResponses(0).digest
        }
    }




    class UploadDirectoryTask(private val node: Node, private val service: RemoteExecutionService) : RecursiveTask<Pair<Node, Digest>>() {
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        override fun compute(): Pair<Node, Digest> {
            val grouped = node.nodes.values.groupBy { it.file }
            val files = ForkJoinTask.invokeAll(grouped[true]?.map { createTask(it, service) } ?: emptyList()).stream().map { it.join() }.toList()
            val dirs = ForkJoinTask.invokeAll(grouped[false]?.map { createTask(it, service) } ?: emptyList()).stream().map { it.join() }.toList()

            val dirBuilder = Directory.newBuilder()
            files.map {
                FileNode.newBuilder().setIsExecutable(true).setName(it.first.name).setDigest(it.second)
                    .build()
            }.let { dirBuilder.addAllFiles(it) }
            dirs.map {
                DirectoryNode.newBuilder().setName(it.first.name).setDigest(it.second).build()
            }.let {
                dirBuilder.addAllDirectories(it)
            }
            val dir = dirBuilder.build()
            val request = BatchUpdateBlobsRequest.Request.newBuilder()
                .setDigest(SHA256.compute(dir))
                .setData(dir.toByteString())
                .build()
            val updateRequest =
                BatchUpdateBlobsRequest.newBuilder().addRequests(request).setInstanceName("remote-execution").build()
            val res = service.upload(updateRequest)
            return node to res.getResponses(0).digest
        }


    }

    private fun reqUpload(node: Node): Digest {
        val forkJoinPool = ForkJoinPool.commonPool()
        return forkJoinPool.invoke(createTask(node, service)).second
    }
}