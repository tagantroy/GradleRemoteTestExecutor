package com.tagantroy.merkletree

import com.tagantroy.merkletree.cache.Cache
import com.tagantroy.merkletree.paths.getExecRootRelPaths
import com.tagantroy.merkletree.types.*
import com.tagantroy.types.InputExclusion
import com.tagantroy.types.InputSpec
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.ArrayDeque
import com.tagantroy.merkletree.types.metadata.Metadata
import java.io.File
import kotlin.math.abs

class MerkleTree(
    val inputSpec: InputSpec,
    val cache: Cache,
) {
    fun computeMerkleTree(
        execRoot: String,
        workingDir: String,
        remoteWorkingDir: String,

        ): PreparedTree {
        val stats = TreeStats(0, 0, 0, 0)
        val fs = mutableMapOf<String, FileSysNode>()
        inputSpec.virtualInputs.forEach {
            if (it.path.isEmpty()) {
                throw IllegalArgumentException() // TODO: Add better reporting
            }
            val absPath = Paths.get(execRoot, it.path)
            val (normPath, remoteNormPath) = getExecRootRelPaths(absPath, execRoot, workingDir, remoteWorkingDir)
            if (it.isEmptyDirectory) {
                if (normPath != ".") {
                    fs[remoteNormPath] = FileSysNode.EmptyDirectory()
                }
            } else {
                val uploadInfoEntry = fromBlob(it.contents)
                val fileNode = FileNode(uploadInfoEntry, it.isExecutable)
                fs[remoteNormPath] = FileSysNode.File(fileNode)
            }
        }
        loadFiles(
            execRoot,
            workingDir,
            remoteWorkingDir,
            inputSpec.inputExclusions,
            inputSpec.inputs,
            fs,
            cache,
            TreeSymlinkOpts(
                preserved = false,
                followsTarget = true
            ) // TODO: handle this properly using inputSpec.symlinkBehavior
        )
        val ft = buildTree(fs)
        val (root, blobs) = packageTree()
        val inputs = blobs.values.toList()
        return PreparedTree(root, inputs, stats)
    }

    private fun loadFiles(
        execRoot: String,
        workingDir: String,
        remoteWorkingDir: String,
        inputExclusions: List<InputExclusion>,
        inputs: List<String>,
        fs: MutableMap<String, FileSysNode>,
        cache: Cache,
        treeSymlinkOpts: TreeSymlinkOpts
    ) {
        val q = ArrayDeque<Path>(inputs.map { Paths.get(it) })
        while (!q.isEmpty()) {
            val path = q.removeFirst()
            val absPath = Paths.get(execRoot, path.toString())
            val (normPath, remoteNormPath) = getExecRootRelPaths(absPath, execRoot, workingDir, remoteWorkingDir)
            val meta = cache.get(absPath)
            when {
                meta.isDirectory -> {
                    if (absPath.toFile().list().isEmpty()) {
                        fs[remoteNormPath] = FileSysNode.EmptyDirectory()
                    }
                    Files.list(absPath).forEach {
                        val curPath = absPath.relativize(it)
                        val res = Paths.get(normPath, it.fileName.toString())
                         q.add(curPath)
                    }
                }
                meta.symlink != null -> {
                    throw RuntimeException()
                }
                meta.isRegularFile -> {
                    fs[remoteNormPath] = handleFile(path, meta)
                }
            }
        }
    }

    private fun handleFile(current: Path, meta: Metadata): FileSysNode {
        val fileNode = FileNode(
            ue = UploadInfoEntry.Path(
                meta.digest, current.toString() // TODO: Use proper path here
            ),
            isExecutable = Files.isExecutable(current)
        )
        return FileSysNode.File(fileNode)
    }

    private fun buildTree(fs: MutableMap<String, FileSysNode>) {
//        val root = TreeNode()
        return null!!
    }

    data class PackageTreeResponse(val root: Digest, val blobs: Map<Digest, UploadInfoEntry>)

    private fun packageTree(): PackageTreeResponse {
        return null!!
    }

    private fun flattenTree(): Map<String, TreeOutput> {
        TODO("Not implemented yet")
    }

    private fun computeOutputsToUpload(): Map<Digest, UploadInfoEntry> {
        TODO("Not implemented yet")
    }

}

data class PreparedTree(val root: Digest, val inputs: List<UploadInfoEntry>, val stats: TreeStats)

