package com.tlpcraft.plugin.commitlint

import com.tlpcraft.plugin.commitlint.CommitLintExtension.Companion.COMMIT_LINT_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class CommitLintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        println("[BUILD-LOGIC] - Applying Commit Lint Convention Plugin")
        with(target) {
            val commitLintExtension = extensions.create<CommitLintExtension>(COMMIT_LINT_EXTENSION_NAME)

            val shouldInstallGitHook = commitLintExtension.shouldInstallGitHook
            if (shouldInstallGitHook.get()) {
                ensureGitHookCommitMsgIsInstalled()
            }

            tasks.register<CommitLintTask>(COMMIT_LINT_TASK_NAME) {
                pattern.set(commitLintExtension.pattern)
                errorMessage.set(commitLintExtension.errorMessage)
                historyLimit.set(commitLintExtension.historyLimit)
                allowMergeCommits.set(commitLintExtension.allowMergeCommits)
                description = "Validates Git commit messages against conventional format"
                group = "verification"
            }
        }
    }

    companion object {
        private const val COMMIT_LINT_TASK_NAME = "commitLintCheck"
    }
}
