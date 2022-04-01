package com.tagantroy.merkletree.paths

import com.tagantroy.merkletree.types.metadata.SymlinkMetadata
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


fun getRelativePath(base: Path, path: Path): Path {
    return base.relativize(path)
}

fun getRemotePath(path: Path, workingDir: String, remoteWorkingDir: String): String {
    val workingDiRelPath = getRelativePath(Paths.get(workingDir), path)
    val remotePath = Paths.get(remoteWorkingDir, workingDiRelPath.toString())
    return remotePath.toString()
}

data class TargetRelPathResponse(val relExecRoot: String, val relSymlinkDir: String)


fun getDir(path: String): String {
    val index = path.lastIndexOf('/')
    if (index == -1) return "."
    return path.substring(0, index)
}

fun getTargetRelPath(execRoot: String, path: String, symMeta: SymlinkMetadata): TargetRelPathResponse {
    val symlinkAbsDir = Paths.get(execRoot, getDir(path))
    var target = symMeta.target
    if (File(target).isAbsolute) {
        target = Paths.get(symlinkAbsDir.toString(), target).toString()
    }
    val relExecRoot = getRelativePath(Paths.get(execRoot), Paths.get(target))
    val relSymlinkDir = getRelativePath(symlinkAbsDir, Paths.get(target))
    return TargetRelPathResponse(
        relExecRoot.toString(), relSymlinkDir.toString()
    )
}

data class GetExecRootRelPathsResponse(val normPath: String, val remoteNormPath: String)

fun getExecRootRelPaths(
    absPath: Path,
    execRoot: String,
    workingDir: String,
    remoteWorkingDir: String
): GetExecRootRelPathsResponse {
    val relPath = getRelativePath(Paths.get(execRoot), absPath)
    if (remoteWorkingDir == "" || remoteWorkingDir == workingDir) {
        return GetExecRootRelPathsResponse(relPath.toString(), relPath.toString())
    }
    val remoteRelPath = getRemotePath(relPath, workingDir, remoteWorkingDir)
    return GetExecRootRelPathsResponse(relPath.toString(), remoteRelPath)
}
