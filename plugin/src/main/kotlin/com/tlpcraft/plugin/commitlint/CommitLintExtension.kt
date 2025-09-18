package com.tlpcraft.plugin.commitlint

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class CommitLintExtension {
    @get:Input
    abstract val pattern: Property<String>

    @get:Input
    abstract val errorMessage: Property<String>

    @get:Input
    abstract val historyLimit: Property<Int>

    @get:Input
    abstract val allowMergeCommits: Property<Boolean>

    @get:Input
    abstract val shouldInstallGitHook: Property<Boolean>

    init {
        pattern.convention(DEFAULT_PATTERN)
        errorMessage.convention(DEFAULT_ERROR_MESSAGE)
        historyLimit.convention(DEFAULT_COMMIT_HISTORY_LIMIT)
        allowMergeCommits.convention(DEFAULT_ALLOW_MERGE_COMMITS)
        shouldInstallGitHook.convention(DEFAULT_INSTALL_GIT_HOOK_FLAG)
    }

    companion object {
        private const val DEFAULT_PATTERN = "^(feat|fix|bugfix|docs|style|refactor|test|chore|ci)(\\(.+\\))?(!)?: .{1,50}$"
        private const val DEFAULT_ERROR_MESSAGE = "Commit message does not follow conventional format: <type>(<scope>): <subject>"
        private const val DEFAULT_COMMIT_HISTORY_LIMIT = 10
        private const val DEFAULT_ALLOW_MERGE_COMMITS = false
        private const val DEFAULT_INSTALL_GIT_HOOK_FLAG = true

        const val COMMIT_LINT_EXTENSION_NAME = "commitLint"
    }
}
