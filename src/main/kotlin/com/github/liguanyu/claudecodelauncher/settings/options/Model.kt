package com.github.liguanyu.claudecodelauncher.settings.options

enum class Model {
    DEFAULT,
    OPUS,
    SONNET,
    HAIKU,
    CUSTOM;

    fun cliName(): String = when (this) {
        DEFAULT -> ""
        OPUS -> "opus"
        SONNET -> "sonnet"
        HAIKU -> "haiku"
        CUSTOM -> ""
    }

    fun toDisplayName(): String = when (this) {
        DEFAULT -> "Default"
        OPUS -> "opus"
        SONNET -> "sonnet"
        HAIKU -> "haiku"
        CUSTOM -> "Custom..."
    }

    override fun toString(): String = toDisplayName()
}
