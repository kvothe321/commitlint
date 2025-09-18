package com.tlpcraft.plugin.commitlint

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations

/**
 * A Gradle task for validating Git commit messages against a regular expression pattern.
 * Validates only the first line (subject) of the commit message.
 */
abstract class CommitLintTask : DefaultTask() {
    /**
     * Regular expression pattern that commit message subjects must match.
     */
    @get:Input
    abstract val pattern: Property<String>

    /**
     * Error message to display when commit message subjects don't match the pattern.
     */
    @get:Input
    abstract val errorMessage: Property<String>

    /**
     * Number of historical commits to check.
     */
    @get:Input
    abstract val historyLimit: Property<Int>

    /**
     * Whether to allow merge commits to bypass validation.
     */
    @get:Input
    abstract val allowMergeCommits: Property<Boolean>

    /**
     * ExecOperations to run Git commands.
     */
    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * Optional path to git repository. Defaults to project directory.
     */
    @get:Input
    @get:Optional
    val repositoryPath = project.objects.property<File>().convention(project.projectDir)

    /**
     * Pattern used to identify merge commits.
     */
    private val mergeCommitPattern = "^Merge .+$".toRegex()

    /**
     * Terminal color codes for error reporting.
     */
    private data class TerminalColors(val enabled: Boolean) {
        val red: String = if (enabled) "\u001B[31m" else ""
        val reset: String = if (enabled) "\u001B[0m" else ""
        val errorIcon: String = if (enabled) "\uD83D\uDEAB " else "✗ "
    }

    @TaskAction
    fun lint() {
        val gitContext = GitContext(repositoryPath.get())

        if (!gitContext.isAvailable()) {
            logger.warn("Git not available or not a Git repository at ${repositoryPath.get().absolutePath}")
            return
        }

        // Check if running from commit-msg hook
        System.getProperty("commit.msg.file")?.let { filePath ->
            val commitMsg = File(filePath).readText().trim()
            validateCommitMessage(commitMsg)
            return
        }

        // Otherwise check historical commits
        val commits = gitContext.getRecentCommits(historyLimit.get())
        validateHistoricalCommits(commits)
    }

    /**
     * Validates a single commit message against the defined pattern,
     * checking only the first line (subject).
     */
    private fun validateCommitMessage(message: String) {
        // Extract only the first line (subject) of the commit message
        val subject = message.split("\n").first().trim()

        // Skip merge commits if they're allowed
        if (subject.matches(mergeCommitPattern) && allowMergeCommits.get()) {
            logger.debug("Skipping merge commit check for merge commit")
            return
        }

        if (!subject.matches(pattern.get().toRegex())) {
            val colors = determineTerminalColorSupport()
            val errorReport = buildString {
                appendLine(errorMessage.get())
                appendLine("Invalid commit message subject:")
                appendLine("${colors.red}${colors.errorIcon}$subject${colors.reset}")
                appendLine("Expected format: ${pattern.get()}")
            }
            throw GradleException(errorReport)
        }
    }

    /**
     * Validates a list of historical commits against the defined pattern,
     * checking only the first line (subject) of each commit.
     */
    private fun validateHistoricalCommits(commits: List<Commit>) {
        val invalidCommits = commits.filterNot { commit ->
            // Extract only the first line (subject) of the commit message
            val subject = commit.message.split("\n").first().trim()

            val isMergeCommit = subject.matches(mergeCommitPattern)
            (isMergeCommit && allowMergeCommits.get()) || subject.matches(pattern.get().toRegex())
        }

        if (invalidCommits.isNotEmpty()) {
            val colors = determineTerminalColorSupport()
            val errorReport = buildString {
                appendLine(errorMessage.get())
                appendLine("Invalid commit message subjects:")
                invalidCommits.forEach {
                    // Extract only the first line (subject) of the commit message
                    val subject = it.message.split("\n").first().trim()
                    appendLine("${colors.red}${colors.errorIcon}${it.hash}: $subject${colors.reset}")
                }
                appendLine("Expected format: ${pattern.get()}")
                if (allowMergeCommits.get()) {
                    appendLine("Note: Merge commits are allowed and bypassing this check")
                }
            }
            throw GradleException(errorReport)
        } else {
            logger.lifecycle("All commit message subjects follow the conventional format")
        }
    }

    /**
     * Determines if terminal supports color output.
     */
    private fun determineTerminalColorSupport(): TerminalColors {
        val noColor = System.getProperty("no.color")?.toBoolean() == true
        val term = System.getenv("TERM") ?: ""
        val supportsColors = !noColor && term != "dumb" && term.isNotEmpty()
        return TerminalColors(supportsColors)
    }

    /**
     * Data class representing a Git commit.
     */
    private data class Commit(val hash: String, val message: String)

    /**
     * Class encapsulating Git operations.
     */
    private inner class GitContext(private val repoDir: File) {
        /**
         * Checks if Git is available and the directory is a Git repository.
         */
        fun isAvailable(): Boolean = isGitInstalled() && isGitRepository()

        /**
         * Retrieves recent commits from the Git repository.
         * Uses a format that captures the full commit message including subject and description.
         */
        fun getRecentCommits(limit: Int): List<Commit> {
            val output = ByteArrayOutputStream()

            execOperations.exec {
                // Use %B instead of %s to get the full commit message
                commandLine("git", "log", "--pretty=format:%h %B", "-n", "$limit")
                standardOutput = output
                workingDir = repoDir
                isIgnoreExitValue = true
            }

            return output.toString().trim().split(Regex("(?<=\n)(?=[a-f0-9]{7} )"))
                .filter { it.isNotEmpty() }
                .map { commitText ->
                    val hashAndMessage = commitText.trim().split(" ", limit = 2)
                    if (hashAndMessage.size == 2) {
                        Commit(hashAndMessage[0], hashAndMessage[1])
                    } else {
                        Commit(hashAndMessage[0], "")
                    }
                }
                .toList()
        }

        /**
         * Checks if Git is installed on the system.
         */
        private fun isGitInstalled(): Boolean = runCatching {
            ProcessBuilder("git", "--version").start().waitFor() == 0
        }.getOrDefault(false)

        /**
         * Checks if the directory is a Git repository.
         */
        private fun isGitRepository(): Boolean = runCatching {
            ProcessBuilder("git", "rev-parse", "--is-inside-work-tree")
                .directory(repoDir)
                .start()
                .waitFor() == 0
        }.getOrDefault(false)
    }
}
