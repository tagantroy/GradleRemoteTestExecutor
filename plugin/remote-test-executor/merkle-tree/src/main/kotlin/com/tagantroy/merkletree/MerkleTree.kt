package com.tagantroy.merkletree

import build.bazel.remote.execution.v2.Directory
import build.bazel.remote.execution.v2.DirectoryNode
import com.tagantroy.merkletree.cache.Cache
import com.tagantroy.merkletree.paths.getExecRootRelPaths
import com.tagantroy.merkletree.types.*
import com.tagantroy.merkletree.types.metadata.Metadata
import com.tagantroy.types.InputExclusion
import com.tagantroy.types.InputSpec
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import build.bazel.remote.execution.v2.FileNode as REFileNode
import build.bazel.remote.execution.v2.SymlinkNode as RESymlinkNode

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
        val (root, blobs) = packageTree(ft, stats)
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
                        q.add(Paths.get(normPath, it.fileName.toString()))
                    }
                }
                meta.symlink != null -> {
                    throw RuntimeException()
                }
                meta.isRegularFile -> {
                    fs[remoteNormPath] = handleFile(absPath, meta)
                }
            }
        }
    }

    private fun handleFile(current: Path, meta: Metadata): FileSysNode {
        val fileNode = FileNode(
            ue = UploadInfoEntry.Path(
                meta.digest, current.toString()
            ),
            isExecutable = Files.isExecutable(current)
        )
        return FileSysNode.File(fileNode)
    }

    private fun buildTree(fs: MutableMap<String, FileSysNode>): TreeNode {
        val root = TreeNode()
        fs.entries.forEach {
            val (name, fn) = it
            val currentPath = Paths.get(name)
            val nameCount = currentPath.nameCount
            currentPath.getName(0)
            val segments = (1..nameCount - 2).map {
                currentPath.getName(it)
            }
            val base = currentPath.getName(nameCount - 1).toString()

            var node = root
            for (seg in segments) {
                node = node.dirs.computeIfAbsent(seg.toString()) { TreeNode() }
            }

            when (fn) {
                is FileSysNode.File -> {
                    node.files[base] = fn.fileNode
                }
                is FileSysNode.EmptyDirectory -> {
                    node.dirs.putIfAbsent(base, TreeNode())
                }
                is FileSysNode.Symlink -> {
                    node.symlinks[base] = fn.symlink
                }
            }
        }
        return root
    }

    data class PackageTreeResponse(val root: Digest, val blobs: Map<Digest, UploadInfoEntry>)

    private fun packageTree(t: TreeNode, stats: TreeStats): PackageTreeResponse {
        val dirBuilder = Directory.newBuilder()
        val blobs = hashMapOf<Digest, UploadInfoEntry>()
        val dirs = mutableListOf<DirectoryNode>()
        for ((name, child) in t.dirs) {
            val (subDirDigest, subDirBlobs) = packageTree(child, stats)
            dirs += DirectoryNode.newBuilder()
                .setName(name)
                .setDigest(subDirDigest.toProto())
                .build()
            blobs.putAll(subDirBlobs)
        }
        dirBuilder.addAllDirectories(dirs.sortedBy { it.name })

        val files = mutableListOf<REFileNode>()
        for ((name, fn) in t.files) {
            val dg = fn.ue.digest
            files += REFileNode.newBuilder().setName(name).setDigest(dg.toProto()).setIsExecutable(fn.isExecutable)
                .build()
            blobs[dg] = fn.ue
            stats.inputFiles++;
            stats.totalInputBytes += dg.size
        }
        dirBuilder.addAllFiles(files.sortedBy { it.name })
        val symlinks = mutableListOf<RESymlinkNode>()
        for ((name, sn) in t.symlinks) {
            symlinks += RESymlinkNode.newBuilder().setName(name).setTarget(sn.target)
                .build()
            stats.inputSymlinks++
        }
        dirBuilder.addAllSymlinks(symlinks.sortedBy { it.name })

        val dir = dirBuilder.build()
        val ue = fromProto(dir)
        val dg = ue.digest
        blobs[dg] = ue
        stats.totalInputBytes += dg.size
        stats.inputDirectories++
        return PackageTreeResponse(dg, blobs)
    }

    private fun flattenTree(): Map<String, TreeOutput> {
        TODO("Not implemented yet")
    }

    private fun computeOutputsToUpload(): Map<Digest, UploadInfoEntry> {
        TODO("Not implemented yet")
    }

}

data class PreparedTree(val root: Digest, val inputs: List<UploadInfoEntry>, val stats: TreeStats)

