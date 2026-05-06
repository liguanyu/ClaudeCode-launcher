package com.github.liguanyu.claudecodelauncher.actions

import com.github.liguanyu.claudecodelauncher.cli.ClaudeCodeHookSettingsFactory
import com.github.liguanyu.claudecodelauncher.cli.CliArgument
import com.github.liguanyu.claudecodelauncher.http.HttpTriggerService
import com.github.liguanyu.claudecodelauncher.settings.ClaudeCodeLauncherSettings
import com.github.liguanyu.claudecodelauncher.settings.options.LaunchShellMode
import com.github.liguanyu.claudecodelauncher.terminal.ClaudeCodeTerminalManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path
import javax.swing.Icon

class LaunchClaudeCodeAction : AnAction(DEFAULT_TEXT, DEFAULT_DESCRIPTION, null), DumbAware {

    companion object {
        private const val CLAUDE_COMMAND = "claude"
        private const val NOTIFICATION_GROUP_ID = "ClaudeCodeLauncher"
        private const val NOTIFICATION_TITLE = "ClaudeCode-launcher"
        private const val DEFAULT_TEXT = "Launch Claude Code"
        private const val DEFAULT_DESCRIPTION = "Open a Claude Code terminal"
        private const val ACTIVE_TEXT = "Insert File Path into Claude Code"
        private const val ACTIVE_DESCRIPTION = "Send the current file path to the Claude Code terminal"
        private val DEFAULT_ICON = IconLoader.getIcon("/icons/claude_code.svg", LaunchClaudeCodeAction::class.java)
        private val ACTIVE_ICON = IconLoader.getIcon("/icons/claude_code_active.svg", LaunchClaudeCodeAction::class.java)
    }

    private val logger = logger<LaunchClaudeCodeAction>()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            logger.warn("No project context available for Claude Code launch")
            return
        }

        val terminalManager = project.service<ClaudeCodeTerminalManager>()
        if (terminalManager.isClaudeCodeTerminalActive()) {
            performInsert(project, terminalManager)
            return
        }

        launchClaudeCode(project, terminalManager)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val state = determineToolbarState(e.project)
        e.presentation.icon = state.icon
        e.presentation.text = state.text
        e.presentation.description = state.description
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    private fun performInsert(project: Project, terminalManager: ClaudeCodeTerminalManager) {
        val payload = InsertPayloadResolver.resolve(project)
        if (payload == null) {
            notify(project, "No active file to send to Claude Code", NotificationType.INFORMATION)
            return
        }

        val insertText = InsertPayloadResolver.formatInsertText(payload)
        if (!terminalManager.typeIntoActiveClaudeCodeTerminal(insertText)) {
            notify(project, "Failed to send file path to Claude Code terminal", NotificationType.WARNING)
            return
        }

        logger.info("Sent active file path to Claude Code terminal: $insertText")
    }

    private fun launchClaudeCode(project: Project, terminalManager: ClaudeCodeTerminalManager) {
        val baseDir = project.basePath ?: System.getProperty("user.home")
        logger.info("Launching Claude Code in directory: $baseDir")

        try {
            val settings = service<ClaudeCodeLauncherSettings>()
            val state = settings.state.copy()
            val generatedSettingsPath = createHookSettingsIfNeeded(project, state)
            val quoteStyle = resolveQuoteStyle(state.launchShellMode)
            val command = buildCommand(settings.getArgTokens(generatedSettingsPath?.toString()), quoteStyle)
            terminalManager.launch(baseDir, command, state)
            logger.info("Claude Code command executed successfully: $command")
        } catch (t: Throwable) {
            logger.error("Failed to launch Claude Code", t)
            notify(project, "Failed to launch Claude Code: ${t.message}", NotificationType.ERROR)
        }
    }

    private fun createHookSettingsIfNeeded(
        project: Project,
        state: ClaudeCodeLauncherSettings.State,
    ): Path? {
        if (!state.enableNotification && !state.openFileOnChange) {
            return null
        }

        val httpService = ApplicationManager.getApplication().service<HttpTriggerService>()
        val port = httpService.getActualPort()
        if (port == 0) {
            notify(project, "HTTP service is not properly initialized", NotificationType.WARNING)
            error("HTTP service is not properly initialized")
        }

        return ClaudeCodeHookSettingsFactory.create(port)
    }

    private fun buildCommand(args: List<CliArgument>, quoteStyle: QuoteStyle): String =
        buildString {
            append(CLAUDE_COMMAND)
            args.forEach { arg ->
                append(' ')
                append(if (arg.raw) arg.value else quote(arg.value, quoteStyle))
            }
        }

    private fun resolveQuoteStyle(mode: LaunchShellMode): QuoteStyle =
        when (mode) {
            LaunchShellMode.WSL -> QuoteStyle.POSIX
            LaunchShellMode.POWERSHELL -> QuoteStyle.POWERSHELL
            LaunchShellMode.FOLLOW_IDE_DEFAULT -> if (SystemInfo.isWindows) QuoteStyle.POWERSHELL else QuoteStyle.POSIX
        }

    private fun quote(value: String, style: QuoteStyle): String {
        if (value.startsWith("--") && value.none { it.isWhitespace() }) {
            return value
        }
        return when (style) {
            QuoteStyle.POSIX -> "'" + value.replace("'", "'\"'\"'") + "'"
            QuoteStyle.POWERSHELL -> "'" + value.replace("'", "''") + "'"
        }
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        runCatching {
            val group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
            group.createNotification(NOTIFICATION_TITLE, content, type).notify(project)
        }.onFailure { error ->
            logger.error("Failed to show notification: $content", error)
        }
    }

    private fun determineToolbarState(project: Project?): ToolbarState {
        if (project == null) {
            return ToolbarState(DEFAULT_ICON, DEFAULT_TEXT, DEFAULT_DESCRIPTION)
        }

        val manager = project.service<ClaudeCodeTerminalManager>()
        return if (manager.isClaudeCodeTerminalActive()) {
            ToolbarState(ACTIVE_ICON, ACTIVE_TEXT, ACTIVE_DESCRIPTION)
        } else {
            ToolbarState(DEFAULT_ICON, DEFAULT_TEXT, DEFAULT_DESCRIPTION)
        }
    }

    private enum class QuoteStyle {
        POSIX,
        POWERSHELL,
    }

    private data class ToolbarState(val icon: Icon, val text: String, val description: String)
}
