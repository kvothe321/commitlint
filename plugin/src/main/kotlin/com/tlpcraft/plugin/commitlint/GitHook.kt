package com.tlpcraft.plugin.commitlint

import java.io.File
import org.gradle.api.Project

fun Project.ensureGitHookCommitMsgIsInstalled() {
    val hookFile = file("${rootProject.projectDir}/.git/hooks/commit-msg")
    val expectedContent = getCommitMsgGitHookScriptContent()

    val needsUpdate = hookFile.exists().not() || hookFile.readText() != expectedContent

    if (needsUpdate) {
        installCommitMsgGitHook(expectedContent)
    } else {
        println("[BUILD LOGIC] - Git commit-msg hook is up-to-date")
    }
}

private fun Project.installCommitMsgGitHook(scriptContent: String) {
    val scriptProvider = resources.text.fromString(scriptContent)
    val hooksDir = file("${rootProject.rootDir}/.git/hooks")
    val destFile = File(hooksDir, "commit-msg")

    copy {
        from(scriptProvider)
        into(hooksDir)
        rename { "commit-msg" }
    }

    destFile.setExecutable(true, false)
    destFile.setReadable(true, false)
    destFile.setWritable(true, false)

    println("[BUILD LOGIC] - Installed commit-msg Git hook for commit message linting")
}
