package com.tagantroy.remoteexecution

data class ExecuteRequest(
    /**
     * An identifier for the command for debugging.
     */
    val commandId: String = "",
    /**
     * An identifier for a group of commands for debugging.
     */
    val invocationId: String = "",
    /**
     * The name of the tool to associate with executed commands.
     */
    val toolName: String = "",
    /**
     * The exec root of the command. The path from which all inputs and outputs are defined relatively.
     */
    val execRoot: String = "",
    /**
     * The working directory, relative to the exec root, for the command to run in.
     * It must be a directory which exists in the input tree.
     * If it is left empty, then the action is run in the exec root.
     */
    val workingDir: String = "",
    /**
     * Comma-separated command input paths, relative to exec root.
     */
    val inputs: List<String>,
    /**
     * Comma-separated command output file paths, relative to exec root.
     */
    val outputFiles: List<String>,
    /**
     * Comma-separated command output directory paths, relative to exec root.
     */
    val outputDirs: List<String>,
    /**
     * Timeout for the command. Value of 0 means no timeout.
     */
    val execTimeout: Long = 0,
    /**
     * Comma-separated key value pairs in the form key=value. This is used to identify remote platform settings like the docker image to use to run the command.
     */
    val platform: Map<String, String>,
    /**
     * Environment variables to pass through to remote execution, as comma-separated key value pairs in the form key=value.
     */
    val environmentVariables: Map<String, String>,
    /**
     * Boolean indicating whether to accept remote cache hits.
     */
    val acceptCached: Boolean = true,
    /**
     * Boolean indicating whether to skip caching the command result remotely.
     */
    val doNotCache: Boolean = false,
    /**
     * Boolean indicating whether to download outputs after the command is executed.
     */
    val downloadOutputs: Boolean = true,
    /**
     * Boolean indicating whether to download stdout and stderr after the command is executed.
     */
    val downloadOutErr: Boolean = true,
)

class Client {
    fun execute(executeRequest: ExecuteRequest) {

    }
}