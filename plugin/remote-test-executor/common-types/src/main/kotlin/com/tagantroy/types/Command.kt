package com.tagantroy.types

import java.time.Duration

data class Command(
    val identifiers: Identifiers,

    // Args (required): command line elements to execute.
    val args: List<String>,

    // ExecRoot is an absolute path to the execution root of the command. All the other paths are
    // specified relatively to this path.
    val execRoot: String,

    // WorkingDir is the working directory, relative to the exec root, for the command to run
    // in. It must be a directory which exists in the input tree. If it is left empty, then the
    // action is run from the exec root.
    val workingDir: String,

    // RemoteWorkingDir is the working directory when executing the command on RE server.
    // It's relative to exec root and, if provided, needs to have the same number of levels
    // as WorkingDir. If not provided, the remote command is run from the WorkingDir
    val remoteWorkingDir: String,

    // InputSpec: the command inputs.
    val inputSpec: InputSpec,

    // OutputFiles are the command output files.
    val outputFiles: List<String>,

    // OutputDirs are the command output directories.
    // The files and directories will likely be merged into a single Outputs field in the future.
    val outputDirs: List<String>,

    // Timeout is an optional duration to wait for command execution before timing out.
    val timeout: Duration,

    // Platform is the platform to use for the execution.
    val platform: Map<String, String>
)