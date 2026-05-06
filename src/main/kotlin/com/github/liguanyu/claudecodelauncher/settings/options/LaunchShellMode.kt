package com.github.liguanyu.claudecodelauncher.settings.options

/**
 * Shell strategy used when opening a Claude Code terminal.
 */
enum class LaunchShellMode {
    FOLLOW_IDE_DEFAULT,
    POWERSHELL,
    WSL;

    fun toDisplayName(): String = when (this) {
        FOLLOW_IDE_DEFAULT -> "Follow IDE default"
        POWERSHELL -> "PowerShell"
        WSL -> "WSL"
    }

    override fun toString(): String = toDisplayName()
}
