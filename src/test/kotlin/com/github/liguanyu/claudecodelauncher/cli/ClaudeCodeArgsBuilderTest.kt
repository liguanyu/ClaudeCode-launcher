package com.github.liguanyu.claudecodelauncher.cli

import com.github.liguanyu.claudecodelauncher.settings.ClaudeCodeLauncherSettings
import com.github.liguanyu.claudecodelauncher.settings.options.Model
import com.github.liguanyu.claudecodelauncher.settings.options.ModelReasoningEffort
import com.github.liguanyu.claudecodelauncher.settings.options.PermissionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClaudeCodeArgsBuilderTest {

    private lateinit var state: ClaudeCodeLauncherSettings.State

    @BeforeEach
    fun setUp() {
        state = ClaudeCodeLauncherSettings.State()
    }

    @Test
    fun `minimal settings produce no args`() {
        assertEquals(emptyList<CliArgument>(), ClaudeCodeArgsBuilder.build(state))
    }

    @Test
    fun `model effort and permission mode become Claude args`() {
        state.model = Model.SONNET
        state.modelReasoningEffort = ModelReasoningEffort.HIGH
        state.permissionMode = PermissionMode.ACCEPT_EDITS

        assertEquals(
            listOf(
                CliArgument("--model"),
                CliArgument("sonnet"),
                CliArgument("--effort"),
                CliArgument("high"),
                CliArgument("--permission-mode"),
                CliArgument("acceptEdits"),
            ),
            ClaudeCodeArgsBuilder.build(state),
        )
    }

    @Test
    fun `custom model and custom effort are trimmed and validated`() {
        state.model = Model.CUSTOM
        state.customModel = "  claude-custom_1  "
        state.modelReasoningEffort = ModelReasoningEffort.CUSTOM
        state.customModelReasoningEffort = "  extra-high  "

        assertEquals(
            listOf(
                CliArgument("--model"),
                CliArgument("claude-custom_1"),
                CliArgument("--effort"),
                CliArgument("extra-high"),
            ),
            ClaudeCodeArgsBuilder.build(state),
        )
    }

    @Test
    fun `unsafe custom values are skipped`() {
        state.model = Model.CUSTOM
        state.customModel = "unsafe'\nmodel"
        state.modelReasoningEffort = ModelReasoningEffort.CUSTOM
        state.customModelReasoningEffort = "bad`value"

        assertEquals(emptyList<CliArgument>(), ClaudeCodeArgsBuilder.build(state))
    }

    @Test
    fun `add dirs split lines and commas`() {
        state.addDirsInput = """
            /project/one, /project/two
            /project three
        """.trimIndent()

        assertEquals(
            listOf(
                CliArgument("--add-dir"),
                CliArgument("/project/one"),
                CliArgument("--add-dir"),
                CliArgument("/project/two"),
                CliArgument("--add-dir"),
                CliArgument("/project three"),
            ),
            ClaudeCodeArgsBuilder.build(state),
        )
    }

    @Test
    fun `tool and config options are passed through`() {
        state.allowedTools = "Bash,Edit"
        state.disallowedTools = "WebFetch"
        state.tools = "Read,Grep"
        state.mcpConfigPath = "/tmp/mcp.json"
        state.settingsPath = "/tmp/settings.json"

        assertEquals(
            listOf(
                CliArgument("--allowedTools"),
                CliArgument("Bash,Edit"),
                CliArgument("--disallowedTools"),
                CliArgument("WebFetch"),
                CliArgument("--tools"),
                CliArgument("Read,Grep"),
                CliArgument("--mcp-config"),
                CliArgument("/tmp/mcp.json"),
                CliArgument("--settings"),
                CliArgument("/tmp/settings.json"),
            ),
            ClaudeCodeArgsBuilder.build(state),
        )
    }

    @Test
    fun `generated settings path overrides configured settings path`() {
        state.settingsPath = "/tmp/user-settings.json"

        assertEquals(
            listOf(
                CliArgument("--settings"),
                CliArgument("/tmp/generated-settings.json"),
            ),
            ClaudeCodeArgsBuilder.build(state, "/tmp/generated-settings.json"),
        )
    }

    @Test
    fun `custom args are appended as raw tail`() {
        state.model = Model.HAIKU
        state.customArgs = """--verbose --some-json '{"x":1}'"""

        assertEquals(
            listOf(
                CliArgument("--model"),
                CliArgument("haiku"),
                CliArgument("""--verbose --some-json '{"x":1}'""", raw = true),
            ),
            ClaudeCodeArgsBuilder.build(state),
        )
    }
}
