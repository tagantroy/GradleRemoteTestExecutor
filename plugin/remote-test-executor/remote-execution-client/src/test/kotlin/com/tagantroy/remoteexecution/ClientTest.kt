package com.tagantroy.remoteexecution

import com.tagantroy.remoteexecution.config.Config
import org.junit.jupiter.api.Test

class ClientTest {
    @Test
    fun runBashCommand() {
        val config = Config(
            "localhost:8790",
            "localhost:8790"
        )
        val client = Client.fromConfig(config)
        val params = Request(
            execRoot = "\$HOME/example",
            workingDir = "foo/bar",
            platform = mapOf("container-image" to ""),
            inputs = listOf("a/hello","a/goodbye"),
            outputFiles = listOf("foo/bar/out"),
        )
        client.execute("/bin/bash -c 'cat hello goodbye > out'", params)
    }

    // Example usage:
// rexec --alsologtostderr --v 1 \
//   --service remotebuildexecution.googleapis.com:443 \
//   --instance $INSTANCE \
//   --credential_file $CRED_FILE \
//   --
}