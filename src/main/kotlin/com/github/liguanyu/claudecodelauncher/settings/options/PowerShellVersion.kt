package com.github.liguanyu.claudecodelauncher.settings.options

/**
 * PowerShell executable family used for explicit PowerShell launches.
 */
enum class PowerShellVersion {
    POWERSHELL_LT_73,
    POWERSHELL_73_PLUS;

    fun defaultExecutable(): String = when (this) {
        POWERSHELL_LT_73 -> "powershell.exe"
        POWERSHELL_73_PLUS -> "pwsh.exe"
    }

    fun toDisplayName(): String = when (this) {
        POWERSHELL_LT_73 -> "PowerShell (< 7.3)"
        POWERSHELL_73_PLUS -> "PowerShell (>= 7.3)"
    }

    override fun toString(): String = toDisplayName()
}
