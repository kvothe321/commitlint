package com.tlpcraft.plugin.commitlint

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

fun getCommitMsgGitHookScriptContent(): String {
    val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    return getScriptContent(isWindows)
}

private fun getScriptContent(isWindows: Boolean) = """
        #!${if (isWindows) "/bin/sh" else "/bin/bash"}
        # Commit message hook to check commit message format
        
        commit_msg_file="$1"
        
        ./gradlew${if (isWindows) ".bat" else ""} commitLintCheck -Dcommit.msg.file="${'$'}commit_msg_file" --daemon
        
        exit_code=$?
        
        if [ ${'$'}exit_code -ne 0 ]; then
            echo ""
            echo "***********************************************************"
            echo "Commit message does not follow the conventional format."
            echo "Please fix your commit message and try again."
            echo "See https://www.conventionalcommits.org for more details."
            echo "***********************************************************"
            exit 1
        else
            exit 0
        fi
    """.trimIndent()
