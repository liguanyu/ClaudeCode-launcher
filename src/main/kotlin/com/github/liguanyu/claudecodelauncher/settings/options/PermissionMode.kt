package com.github.liguanyu.claudecodelauncher.settings.options

/**
 * Claude Code permission mode for the `--permission-mode` argument.
 */
enum class PermissionMode {
    DEFAULT,
    CLAUDE_DEFAULT,
    ACCEPT_EDITS,
    BYPASS_PERMISSIONS,
    PLAN;

    fun cliName(): String = when (this) {
        DEFAULT -> ""
        CLAUDE_DEFAULT -> "default"
        ACCEPT_EDITS -> "acceptEdits"
        BYPASS_PERMISSIONS -> "bypassPermissions"
        PLAN -> "plan"
    }

    fun toDisplayName(): String = when (this) {
        DEFAULT -> "Default (no argument)"
        CLAUDE_DEFAULT -> "Claude default"
        ACCEPT_EDITS -> "Accept edits"
        BYPASS_PERMISSIONS -> "Bypass permissions"
        PLAN -> "Plan"
    }

    override fun toString(): String = toDisplayName()
}
