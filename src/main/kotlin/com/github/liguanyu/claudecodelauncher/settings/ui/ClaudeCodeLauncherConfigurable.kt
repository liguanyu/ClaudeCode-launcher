package com.github.liguanyu.claudecodelauncher.settings.ui

import com.github.liguanyu.claudecodelauncher.settings.ClaudeCodeLauncherSettings
import com.github.liguanyu.claudecodelauncher.settings.options.LaunchShellMode
import com.github.liguanyu.claudecodelauncher.settings.options.Model
import com.github.liguanyu.claudecodelauncher.settings.options.ModelReasoningEffort
import com.github.liguanyu.claudecodelauncher.settings.options.PermissionMode
import com.github.liguanyu.claudecodelauncher.settings.options.PowerShellVersion
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JComboBox
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class ClaudeCodeLauncherConfigurable : SearchableConfigurable {
    private lateinit var root: JComponent
    private lateinit var modelCombo: JComboBox<Model>
    private lateinit var customModelField: JBTextField
    private lateinit var effortCombo: JComboBox<ModelReasoningEffort>
    private lateinit var customEffortField: JBTextField
    private lateinit var permissionModeCombo: JComboBox<PermissionMode>
    private lateinit var addDirsArea: JBTextArea
    private lateinit var allowedToolsField: JBTextField
    private lateinit var disallowedToolsField: JBTextField
    private lateinit var toolsField: JBTextField
    private lateinit var mcpConfigPathField: JBTextField
    private lateinit var settingsPathField: JBTextField
    private lateinit var customArgsField: JBTextField
    private lateinit var openFileOnChangeCheckbox: JBCheckBox
    private lateinit var enableNotificationCheckbox: JBCheckBox
    private lateinit var launchShellModeCombo: JComboBox<LaunchShellMode>
    private lateinit var powerShellVersionCombo: JComboBox<PowerShellVersion>
    private lateinit var powerShellPathField: JBTextField
    private lateinit var wslPathField: JBTextField
    private lateinit var wslDistroField: JBTextField

    private val settings by lazy { service<ClaudeCodeLauncherSettings>() }

    companion object {
        private val ALLOWED_SAFE_VALUE_REGEX = Regex("^[A-Za-z0-9._-]*$")
        private const val SHORT_FIELD_COLUMNS = 24
        private const val LONG_FIELD_COLUMNS = 56
    }

    override fun getId(): String = "com.github.liguanyu.claudecodelauncher.settings"

    override fun getDisplayName(): String = "ClaudeCode-launcher"

    override fun createComponent(): JComponent {
        modelCombo = ComboBox(Model.entries.toTypedArray(), 180)
        customModelField = JBTextField().apply {
            emptyText.text = "e.g. sonnet"
            columns = SHORT_FIELD_COLUMNS
            isEnabled = false
        }
        effortCombo = ComboBox(ModelReasoningEffort.entries.toTypedArray(), 160)
        customEffortField = JBTextField().apply {
            emptyText.text = "e.g. high"
            columns = SHORT_FIELD_COLUMNS
            isEnabled = false
        }
        permissionModeCombo = ComboBox(PermissionMode.entries.toTypedArray(), 200)
        addDirsArea = JBTextArea(3, 50).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            emptyText.text = "One --add-dir path per line"
        }
        allowedToolsField = longTextField("e.g. Bash,Edit")
        disallowedToolsField = longTextField("e.g. WebFetch")
        toolsField = longTextField("Tool list passed to --tools")
        mcpConfigPathField = longTextField("Path to MCP config JSON")
        settingsPathField = longTextField("Path to Claude settings JSON")
        customArgsField = JBTextField().apply {
            emptyText.text = "Additional Claude Code arguments"
            columns = LONG_FIELD_COLUMNS
        }
        openFileOnChangeCheckbox = JBCheckBox("Open files automatically when changed")
        enableNotificationCheckbox = JBCheckBox("Enable notifications when Claude Code stops")
        launchShellModeCombo = ComboBox(LaunchShellMode.entries.toTypedArray(), 200)
        powerShellVersionCombo = ComboBox(PowerShellVersion.entries.toTypedArray(), 180)
        powerShellPathField = longTextField("Optional; defaults to powershell.exe or pwsh.exe")
        wslPathField = longTextField("Optional; defaults to wsl.exe")
        wslDistroField = longTextField("Optional WSL distro name")

        installSafeValueFilter(customModelField)
        installSafeValueFilter(customEffortField)
        modelCombo.addActionListener { customModelField.isEnabled = getModel() == Model.CUSTOM }
        effortCombo.addActionListener { customEffortField.isEnabled = getEffort() == ModelReasoningEffort.CUSTOM }
        launchShellModeCombo.addActionListener { updateShellFields() }

        root = panel {
            group("Launch Shell") {
                row("Shell") { cell(launchShellModeCombo) }
                row("PowerShell version") { cell(powerShellVersionCombo) }
                row("PowerShell executable") { cell(powerShellPathField).resizableColumn().align(AlignX.FILL) }
                row("WSL executable") { cell(wslPathField).resizableColumn().align(AlignX.FILL) }
                row("WSL distro") { cell(wslDistroField).resizableColumn().align(AlignX.FILL) }
            }
            group("Model") {
                row("Model") { cell(modelCombo) }
                row("Custom model") { cell(customModelField).resizableColumn().align(AlignX.FILL) }
                row("Effort") { cell(effortCombo) }
                row("Custom effort") { cell(customEffortField).resizableColumn().align(AlignX.FILL) }
            }
            group("Permissions") {
                row("Permission mode") { cell(permissionModeCombo) }
            }
            group("Claude Code Arguments") {
                row("Add dirs") { cell(JBScrollPane(addDirsArea)).resizableColumn().align(AlignX.FILL) }
                row("Allowed tools") { cell(allowedToolsField).resizableColumn().align(AlignX.FILL) }
                row("Disallowed tools") { cell(disallowedToolsField).resizableColumn().align(AlignX.FILL) }
                row("Tools") { cell(toolsField).resizableColumn().align(AlignX.FILL) }
                row("MCP config") { cell(mcpConfigPathField).resizableColumn().align(AlignX.FILL) }
                row("Settings") { cell(settingsPathField).resizableColumn().align(AlignX.FILL) }
                row("Custom args") { cell(customArgsField).resizableColumn().align(AlignX.FILL) }
            }
            group("Hooks") {
                row { cell(enableNotificationCheckbox) }
                row { cell(openFileOnChangeCheckbox) }
            }
        }

        updateShellFields()
        return root
    }

    private fun longTextField(emptyText: String): JBTextField =
        JBTextField().apply {
            this.emptyText.text = emptyText
            columns = LONG_FIELD_COLUMNS
        }

    override fun isModified(): Boolean {
        val s = settings.state
        return getModel() != s.model ||
            getCustomModel() != s.customModel ||
            getEffort() != s.modelReasoningEffort ||
            getCustomEffort() != s.customModelReasoningEffort ||
            getPermissionMode() != s.permissionMode ||
            getAddDirsInput() != s.addDirsInput ||
            getAllowedTools() != s.allowedTools ||
            getDisallowedTools() != s.disallowedTools ||
            getTools() != s.tools ||
            getMcpConfigPath() != s.mcpConfigPath ||
            getSettingsPath() != s.settingsPath ||
            getCustomArgs() != s.customArgs ||
            getOpenFileOnChange() != s.openFileOnChange ||
            getEnableNotification() != s.enableNotification ||
            getLaunchShellMode() != s.launchShellMode ||
            getPowerShellVersion() != s.powerShellVersion ||
            getPowerShellPath() != s.powerShellExecutablePath ||
            getWslPath() != s.wslExecutablePath ||
            getWslDistro() != s.wslDistro
    }

    override fun apply() {
        validateCustomValue(getModel() == Model.CUSTOM, getCustomModel(), "Custom model is required.")
        validateCustomValue(
            getEffort() == ModelReasoningEffort.CUSTOM,
            getCustomEffort(),
            "Custom effort is required.",
        )

        val s = settings.state
        s.model = getModel()
        s.customModel = getCustomModel()
        s.modelReasoningEffort = getEffort()
        s.customModelReasoningEffort = getCustomEffort()
        s.permissionMode = getPermissionMode()
        s.addDirsInput = getAddDirsInput()
        s.allowedTools = getAllowedTools()
        s.disallowedTools = getDisallowedTools()
        s.tools = getTools()
        s.mcpConfigPath = getMcpConfigPath()
        s.settingsPath = getSettingsPath()
        s.customArgs = getCustomArgs()
        s.openFileOnChange = getOpenFileOnChange()
        s.enableNotification = getEnableNotification()
        s.launchShellMode = getLaunchShellMode()
        s.powerShellVersion = getPowerShellVersion()
        s.powerShellExecutablePath = getPowerShellPath()
        s.wslExecutablePath = getWslPath()
        s.wslDistro = getWslDistro()
    }

    override fun reset() {
        val s = settings.state
        modelCombo.selectedItem = s.model
        customModelField.text = s.customModel
        customModelField.isEnabled = s.model == Model.CUSTOM
        effortCombo.selectedItem = s.modelReasoningEffort
        customEffortField.text = s.customModelReasoningEffort
        customEffortField.isEnabled = s.modelReasoningEffort == ModelReasoningEffort.CUSTOM
        permissionModeCombo.selectedItem = s.permissionMode
        addDirsArea.text = s.addDirsInput
        allowedToolsField.text = s.allowedTools
        disallowedToolsField.text = s.disallowedTools
        toolsField.text = s.tools
        mcpConfigPathField.text = s.mcpConfigPath
        settingsPathField.text = s.settingsPath
        customArgsField.text = s.customArgs
        openFileOnChangeCheckbox.isSelected = s.openFileOnChange
        enableNotificationCheckbox.isSelected = s.enableNotification
        launchShellModeCombo.selectedItem = s.launchShellMode
        powerShellVersionCombo.selectedItem = s.powerShellVersion
        powerShellPathField.text = s.powerShellExecutablePath
        wslPathField.text = s.wslExecutablePath
        wslDistroField.text = s.wslDistro
        updateShellFields()
    }

    private fun installSafeValueFilter(field: JBTextField) {
        (field.document as? AbstractDocument)?.documentFilter = object : DocumentFilter() {
            override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: AttributeSet?) {
                if (string == null) return
                val current = fb.document.getText(0, fb.document.length)
                val next = StringBuilder(current).insert(offset, string).toString()
                if (ALLOWED_SAFE_VALUE_REGEX.matches(next)) {
                    super.insertString(fb, offset, string, attr)
                }
            }

            override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
                val current = fb.document.getText(0, fb.document.length)
                val next = StringBuilder(current).replace(offset, offset + length, text ?: "").toString()
                if (ALLOWED_SAFE_VALUE_REGEX.matches(next)) {
                    super.replace(fb, offset, length, text, attrs)
                }
            }
        }
    }

    private fun updateShellFields() {
        if (!::powerShellVersionCombo.isInitialized) return
        val mode = getLaunchShellMode()
        powerShellVersionCombo.isEnabled = mode == LaunchShellMode.POWERSHELL
        powerShellPathField.isEnabled = mode == LaunchShellMode.POWERSHELL
        wslPathField.isEnabled = mode == LaunchShellMode.WSL
        wslDistroField.isEnabled = mode == LaunchShellMode.WSL
    }

    private fun validateCustomValue(isCustomSelected: Boolean, value: String, message: String) {
        if (isCustomSelected && (value.isBlank() || !ALLOWED_SAFE_VALUE_REGEX.matches(value))) {
            throw ConfigurationException("$message Use only letters, digits, '.', '-', and '_'.")
        }
    }

    private fun getModel(): Model = (modelCombo.selectedItem as? Model) ?: Model.DEFAULT
    private fun getCustomModel(): String = customModelField.text?.trim().orEmpty()
    private fun getEffort(): ModelReasoningEffort =
        (effortCombo.selectedItem as? ModelReasoningEffort) ?: ModelReasoningEffort.DEFAULT
    private fun getCustomEffort(): String = customEffortField.text?.trim().orEmpty()
    private fun getPermissionMode(): PermissionMode =
        (permissionModeCombo.selectedItem as? PermissionMode) ?: PermissionMode.DEFAULT
    private fun getAddDirsInput(): String = addDirsArea.text.orEmpty().trim()
    private fun getAllowedTools(): String = allowedToolsField.text?.trim().orEmpty()
    private fun getDisallowedTools(): String = disallowedToolsField.text?.trim().orEmpty()
    private fun getTools(): String = toolsField.text?.trim().orEmpty()
    private fun getMcpConfigPath(): String = mcpConfigPathField.text?.trim().orEmpty()
    private fun getSettingsPath(): String = settingsPathField.text?.trim().orEmpty()
    private fun getCustomArgs(): String = customArgsField.text?.trim().orEmpty()
    private fun getOpenFileOnChange(): Boolean = openFileOnChangeCheckbox.isSelected
    private fun getEnableNotification(): Boolean = enableNotificationCheckbox.isSelected
    private fun getLaunchShellMode(): LaunchShellMode =
        (launchShellModeCombo.selectedItem as? LaunchShellMode) ?: LaunchShellMode.FOLLOW_IDE_DEFAULT
    private fun getPowerShellVersion(): PowerShellVersion =
        (powerShellVersionCombo.selectedItem as? PowerShellVersion) ?: PowerShellVersion.POWERSHELL_LT_73
    private fun getPowerShellPath(): String = powerShellPathField.text?.trim().orEmpty()
    private fun getWslPath(): String = wslPathField.text?.trim().orEmpty()
    private fun getWslDistro(): String = wslDistroField.text?.trim().orEmpty()
}
