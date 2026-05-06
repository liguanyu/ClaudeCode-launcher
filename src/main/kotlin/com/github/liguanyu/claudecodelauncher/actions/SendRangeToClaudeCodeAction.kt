package com.github.liguanyu.claudecodelauncher.actions

import com.github.liguanyu.claudecodelauncher.terminal.ClaudeCodeTerminalManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader

class SendRangeToClaudeCodeAction : AnAction(
    "Send Selection/Class to Claude Code",
    "Send the highlighted range or enclosing class lines to the Claude Code terminal",
    IconLoader.getIcon("/icons/claude_code_active.svg", SendRangeToClaudeCodeAction::class.java)
), DumbAware {

    companion object {
        private const val NOTIFICATION_TITLE = "ClaudeCode-launcher"
    }

    private val logger = logger<SendRangeToClaudeCodeAction>()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (project == null || editor == null) {
            return
        }

        val payload = InsertPayloadResolver.resolve(
            project = project,
            editor = editor,
            file = virtualFile
        )

        if (payload == null) {
            notify(project, "Unable to determine selection or enclosing class", NotificationType.INFORMATION)
            return
        }

        val insertText = InsertPayloadResolver.formatInsertText(payload)
        val terminalManager = project.service<ClaudeCodeTerminalManager>()
        if (!terminalManager.isClaudeCodeTerminalActive()) {
            notify(project, "Launch Claude Code first to send ranges", NotificationType.INFORMATION)
            return
        }

        if (!terminalManager.typeIntoActiveClaudeCodeTerminal(insertText)) {
            notify(project, "Failed to send range to Claude Code terminal", NotificationType.WARNING)
            return
        }

        logger.info("Sent editor range to Claude Code terminal: $insertText")
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (project == null || editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val terminalManager = project.service<ClaudeCodeTerminalManager>()
        if (!terminalManager.isClaudeCodeTerminalActive()) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val hasSelection = editor.selectionModel.hasSelection()
        val inContextBar = e.place == "EditorContextBar"

        val visible = !inContextBar || hasSelection
        e.presentation.isVisible = visible
        e.presentation.isEnabled = visible && (hasSelection || !inContextBar)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    private fun notify(project: Project, content: String, type: NotificationType) {
        runCatching {
            val group = NotificationGroupManager.getInstance().getNotificationGroup("ClaudeCodeLauncher")
            group.createNotification(NOTIFICATION_TITLE, content, type).notify(project)
        }.onFailure { error ->
            logger.warn("Failed to display notification: $content", error)
        }
    }
}
