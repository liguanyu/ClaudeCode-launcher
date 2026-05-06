package com.github.liguanyu.claudecodelauncher.settings

import com.github.liguanyu.claudecodelauncher.cli.ClaudeCodeArgsBuilder
import com.github.liguanyu.claudecodelauncher.cli.CliArgument
import com.github.liguanyu.claudecodelauncher.settings.options.LaunchShellMode
import com.github.liguanyu.claudecodelauncher.settings.options.Model
import com.github.liguanyu.claudecodelauncher.settings.options.ModelReasoningEffort
import com.github.liguanyu.claudecodelauncher.settings.options.PermissionMode
import com.github.liguanyu.claudecodelauncher.settings.options.PowerShellVersion
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings service for ClaudeCode-launcher.
 */
@Service(Service.Level.APP)
@State(name = "ClaudeCodeLauncherSettings", storages = [Storage("ClaudeCodeLauncher.xml")])
class ClaudeCodeLauncherSettings : PersistentStateComponent<ClaudeCodeLauncherSettings.State> {
    data class State(
        var model: Model = Model.DEFAULT,
        var customModel: String = "",
        var modelReasoningEffort: ModelReasoningEffort = ModelReasoningEffort.DEFAULT,
        var customModelReasoningEffort: String = "",
        var permissionMode: PermissionMode = PermissionMode.DEFAULT,
        var addDirsInput: String = "",
        var allowedTools: String = "",
        var disallowedTools: String = "",
        var tools: String = "",
        var mcpConfigPath: String = "",
        var settingsPath: String = "",
        var openFileOnChange: Boolean = false,
        var enableNotification: Boolean = false,
        var customArgs: String = "",
        var launchShellMode: LaunchShellMode = LaunchShellMode.FOLLOW_IDE_DEFAULT,
        var powerShellVersion: PowerShellVersion = PowerShellVersion.POWERSHELL_LT_73,
        var powerShellExecutablePath: String = "",
        var wslExecutablePath: String = "",
        var wslDistro: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun getArgTokens(generatedSettingsPath: String? = null): List<CliArgument> =
        ClaudeCodeArgsBuilder.build(state, generatedSettingsPath)
}
