package com.tagantroy.remoteexecutionplugin

import build.bazel.remote.execution.v2.*
import com.google.common.hash.HashCode
import com.google.common.hash.HashingInputStream
import com.google.protobuf.ByteString
import com.tagantroy.remoteexecutionplugin.service.DigestUtil
import com.tagantroy.remoteexecutionplugin.service.RemoteExecutionService
import com.tagantroy.remoteexecutionplugin.service.SHA256
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.readBytes
import kotlin.math.log

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

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class FakeFileTree() {
    val root = Node("root", "root", false)
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

    fun calculateHashForFile(path: Path): HashCode {
        return SHA256.hash(path)
    }

    fun calculateHashes() {
        reqCalculateHash(root);
    }

    fun reqCalculateHash(node: Node) {
        if (node.file) {
            return
        } else {
            node.nodes.forEach { t, u ->
                reqCalculateHash(u)
            }
            node.hash =
                SHA256.hashUnordered(listOf(SHA256.hash(node.currentVirtualPath)) + node.nodes.values.map { it.hash }
                    .filterNotNull().toList())
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
            logger.error("Cannot relativize")
            file.toPath()
        }
    }
}

class FileManager(
    private val rootProjectDir: File,
    private val buildDir: File,
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

        return uploadTree(tree.root)
    }

    private fun uploadTree(root: Node): Digest {
        return reqUpload(root)
//        root.nodes.forEach { t, u -> reqUpload(u) }
    }


    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private fun reqUpload(node: Node): Digest {
        if (node.file) {
            val dataRequest = BatchUpdateBlobsRequest.Request.newBuilder()
                .setDigest(SHA256.compute(node.hash!!, node.sizeBytes!!))
//                .setData(ByteString.readFrom(node.path!!.inputStream()))
                .setData( ByteString.copyFrom(node.path!!.readBytes()))
                .build()


//            val updateRequest = BatchUpdateBlobsRequest.newBuilder().addRequests(dataRequest).addRequests(fileNodeRequest).build()
            val updateRequest =
                BatchUpdateBlobsRequest.newBuilder().addRequests(dataRequest).setInstanceName("remote-execution")
                    .build()
            val res = service.upload(updateRequest)
            logger.info("Upload file: ${node.currentVirtualPath} with res: $res")
            return res.getResponses(0).digest
        } else {

            node.nodes.forEach { (_, u) -> reqUpload(u) }
            node.sizeBytes = node.nodes.values.sumOf { it.sizeBytes!! }
            val grouped = node.nodes.values.groupBy { it.file }
            val files = grouped[true]
            val dirs = grouped[false]
            val dirBuilder = Directory.newBuilder()
            files?.map {
                FileNode.newBuilder().setIsExecutable(true).setName(node.name).setDigest(SHA256.compute(node.hash!!))
                    .build()
            }?.let { dirBuilder.addAllFiles(it) }
            dirs?.map {
                DirectoryNode.newBuilder().setName(node.name).setDigest(SHA256.compute(node.hash!!)).build()
            }?.let {
                dirBuilder.addAllDirectories(it)
            }
            val dir = dirBuilder.build()
            val request = BatchUpdateBlobsRequest.Request.newBuilder()
                .setDigest(SHA256.compute(dir, node.sizeBytes!!))
                .setData(dir.toByteString())
                .build()
            val updateRequest =
                BatchUpdateBlobsRequest.newBuilder().addRequests(request).setInstanceName("remote-execution").build()
            val res = service.upload(updateRequest)
            logger.info("Upload dir: $dir with res: $res")
            return res.getResponses(0).digest
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