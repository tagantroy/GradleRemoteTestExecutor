package com.tagantroy.merkletree

import com.tagantroy.merkletree.types.*

class MerkleTree {
    fun computeMerkleTree(
        execRoot: String,
        workingDir: String,
        remoteWorkingDir: String,
    ): PreparedTree {
        val stats = TreeStats(0,0,0,0)
        val fs = mutableMapOf<String, FileSysNode>()

//    fs := make(map[string]*fileSysNode)
//    for _, i := range is.VirtualInputs {
//        if i.Path == "" {
//            return digest.Empty, nil, nil, errors.New("empty Path in VirtualInputs")
//        }
//        absPath := filepath.Join(execRoot, i.Path)
//        normPath, remoteNormPath, err := getExecRootRelPaths(absPath, execRoot, workingDir, remoteWorkingDir)
//        if err != nil {
//            return digest.Empty, nil, nil, err
//        }
//        if i.IsEmptyDirectory {
//            if normPath != "." {
//                fs[remoteNormPath] = &fileSysNode{emptyDirectoryMarker: true}
//            }
//            continue
//        }
//        fs[remoteNormPath] = &fileSysNode{
//            file: &fileNode{
//            ue:           uploadinfo.EntryFromBlob(i.Contents),
//            isExecutable: i.IsExecutable,
//        },
//        }
//    }
//    if err := loadFiles(execRoot, workingDir, remoteWorkingDir, is.InputExclusions, is.Inputs, fs, cache, treeSymlinkOpts(c.TreeSymlinkOpts, is.SymlinkBehavior)); err != nil {
//        return digest.Empty, nil, nil, err
//    }
//    ft, err := buildTree(fs)
//    if err != nil {
//        return digest.Empty, nil, nil, err
//    }
//    var blobs map[digest.Digest]*uploadinfo.Entry
//    root, blobs, err = packageTree(ft, stats)
//    if err != nil {
//        return digest.Empty, nil, nil, err
//    }
//    for _, ue := range blobs {
//        inputs = append(inputs, ue)
//    }
//    return root, inputs, stats, nil

//        val opts = TreeSymlinkOpts(false, inputSpec.symlinkBehavior);
//        val inputExclusions = emptyList<String>()
//        loadFiles(execRoot, workingDir, remoteWorkingDir, inputs, fs, cache, opts )
        val root = Digest("", 0)
        val inputs = listOf<UploadInfoEntry>()
        return PreparedTree(root, inputs, stats)
    }
    private fun loadFiles() {

    }
    private fun buildTree() {

    }

    private fun packageTree() {

    }

    private fun flattenTree(): Map<String, TreeOutput> {
        TODO("Not implemented yet")
    }

    private fun computeOutputsToUpload(): Map<Digest, UploadInfoEntry> {
        TODO("Not implemented yet")
    }

}

data class PreparedTree(val root: Digest, val inputs: List<UploadInfoEntry>, val stats: TreeStats)

