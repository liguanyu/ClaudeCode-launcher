package com.github.liguanyu.claudecodelauncher.settings.options

/**
 * Model reasoning effort choices.
 *
 * `CUSTOM` is a display/persistence marker only. It is not a direct CLI token:
 * - `cliName()` returns an empty string for `CUSTOM`
 * - `toDisplayName()` renders `CUSTOM` as "Custom..."
 * - callers must resolve and validate a separate persisted custom value before CLI use
 */
enum class ModelReasoningEffort {
    DEFAULT,
    LOW,
    MEDIUM,
    HIGH,
    CUSTOM;

    fun cliName(): String = when (this) {
        DEFAULT -> ""
        LOW -> "low"
        MEDIUM -> "medium"
        HIGH -> "high"
        CUSTOM -> ""
    }

    fun toDisplayName(): String = when (this) {
        DEFAULT -> "Default"
        LOW -> "Low"
        MEDIUM -> "Medium"
        HIGH -> "High"
        CUSTOM -> "Custom..."
    }

    override fun toString(): String = toDisplayName()
}
