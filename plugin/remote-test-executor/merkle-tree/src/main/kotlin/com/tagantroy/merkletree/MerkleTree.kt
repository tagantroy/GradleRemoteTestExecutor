package com.tagantroy.merkletree

import com.tagantroy.merkletree.types.Digest
import com.tagantroy.merkletree.types.TreeOutput
import com.tagantroy.merkletree.types.TreeStats
import com.tagantroy.merkletree.types.UploadInfoEntry

class MerkleTree {

}



data class PreparedTree(val root: Digest, val inputs: List<UploadInfoEntry>, val stats: TreeStats)




fun computeMerkleTree() {

}

fun flattenTree(): Map<String, TreeOutput> {
    TODO("Not implemented yet")
}

fun computeOutputsToUpload(): Map<Digest, UploadInfoEntry> {
    TODO("Not implemented yet")
}

