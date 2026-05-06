package com.github.liguanyu.claudecodelauncher.cli

import com.github.liguanyu.claudecodelauncher.settings.ClaudeCodeLauncherSettings
import com.github.liguanyu.claudecodelauncher.settings.options.Model
import com.github.liguanyu.claudecodelauncher.settings.options.ModelReasoningEffort
import com.github.liguanyu.claudecodelauncher.settings.options.PermissionMode
import com.intellij.openapi.util.SystemInfo

data class CliArgument(val value: String, val raw: Boolean = false)

interface OsProvider {
    val isWindows: Boolean
}

object DefaultOsProvider : OsProvider {
    override val isWindows: Boolean get() = SystemInfo.isWindows
}

object ClaudeCodeArgsBuilder {
    private val ALLOWED_SAFE_CLI_VALUE_REGEX = Regex("^[A-Za-z0-9._-]+$")

    fun build(
        state: ClaudeCodeLauncherSettings.State,
        generatedSettingsPath: String? = null,
    ): List<CliArgument> {
        val parts = mutableListOf<CliArgument>()

        val modelName = when (state.model) {
            Model.DEFAULT -> null
            Model.CUSTOM -> sanitizeCliValue(state.customModel)
            else -> state.model.cliName()
        }
        if (modelName != null) {
            parts += option("--model", modelName)
        }

        val effort = when (state.modelReasoningEffort) {
            ModelReasoningEffort.DEFAULT -> null
            ModelReasoningEffort.CUSTOM -> sanitizeCliValue(state.customModelReasoningEffort)
            else -> state.modelReasoningEffort.cliName()
        }
        if (effort != null) {
            parts += option("--effort", effort)
        }

        val permissionMode = when (state.permissionMode) {
            PermissionMode.DEFAULT -> null
            else -> state.permissionMode.cliName()
        }
        if (permissionMode != null) {
            parts += option("--permission-mode", permissionMode)
        }

        splitMultilineValues(state.addDirsInput).forEach { addDir ->
            parts += option("--add-dir", addDir)
        }

        addStringOption(parts, "--allowedTools", state.allowedTools)
        addStringOption(parts, "--disallowedTools", state.disallowedTools)
        addStringOption(parts, "--tools", state.tools)
        addStringOption(parts, "--mcp-config", state.mcpConfigPath)
        addStringOption(parts, "--settings", generatedSettingsPath ?: state.settingsPath)

        val customArgs = state.customArgs.trim()
        if (customArgs.isNotEmpty()) {
            parts += CliArgument(customArgs, raw = true)
        }

        return parts
    }

    private fun addStringOption(parts: MutableList<CliArgument>, optionName: String, value: String?) {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            parts += option(optionName, trimmed)
        }
    }

    private fun option(name: String, value: String): List<CliArgument> =
        listOf(CliArgument(name), CliArgument(value))

    private fun sanitizeCliValue(value: String): String? {
        val trimmedValue = value.trim()
        if (trimmedValue.isEmpty()) {
            return null
        }
        return trimmedValue.takeIf { ALLOWED_SAFE_CLI_VALUE_REGEX.matches(it) }
    }

    private fun splitMultilineValues(value: String): List<String> =
        value.lineSequence()
            .flatMap { line -> line.split(',').asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
}
