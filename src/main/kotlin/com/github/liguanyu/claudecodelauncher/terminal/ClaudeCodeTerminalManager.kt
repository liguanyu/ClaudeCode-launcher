package com.github.liguanyu.claudecodelauncher.terminal

import com.github.liguanyu.claudecodelauncher.settings.ClaudeCodeLauncherSettings
import com.github.liguanyu.claudecodelauncher.settings.options.LaunchShellMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

@Service(Service.Level.PROJECT)
class ClaudeCodeTerminalManager(private val project: Project) {

    companion object {
        private const val TAB_NAME = "Claude Code"
        private val CLAUDE_CODE_TERMINAL_KEY = Key.create<Boolean>("claudecode.launcher.terminal")
        private val CLAUDE_CODE_TERMINAL_RUNNING_KEY = Key.create<Boolean>("claudecode.launcher.terminal.running")
        private val CLAUDE_CODE_TERMINAL_CALLBACK_KEY = Key.create<Boolean>("claudecode.launcher.terminal.callbackRegistered")
        private val CLAUDE_CODE_TERMINAL_SHELL_KEY = Key.create<String>("claudecode.launcher.terminal.shell")
    }

    private val logger = logger<ClaudeCodeTerminalManager>()
    private val scriptFactory = CommandScriptFactory(project)

    private data class ClaudeCodeTerminal(val widget: TerminalWidget, val content: Content)

    fun launch(baseDir: String, command: String, state: ClaudeCodeLauncherSettings.State) {
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val explicitShellCommand = ShellLaunchCommandBuilder.shellCommand(state)
        val shellIdentity = ShellLaunchCommandBuilder.shellIdentity(state)
        var existingTerminal = locateClaudeCodeTerminal(terminalManager)

        existingTerminal?.let { terminal ->
            ensureTerminationCallback(terminal.widget, terminal.content)
            if (terminal.content.getUserData(CLAUDE_CODE_TERMINAL_SHELL_KEY) != shellIdentity) {
                logger.info("Creating a new Claude Code terminal because the configured launch shell changed")
                clearClaudeCodeMetadata(terminal.content)
                existingTerminal = null
                return@let
            }
            if (isClaudeCodeRunning(terminal)) {
                logger.info("Focusing active Claude Code terminal")
                focusClaudeCodeTerminal(terminalManager, terminal)
                return
            }

            if (reuseClaudeCodeTerminal(terminal, command)) {
                logger.info("Reused existing Claude Code terminal")
                focusClaudeCodeTerminal(terminalManager, terminal)
                return
            } else {
                clearClaudeCodeMetadata(terminalManager, terminal.widget)
                existingTerminal = null
            }
        }

        var widget: TerminalWidget? = null
        try {
            widget = if (explicitShellCommand == null) {
                terminalManager.createShellWidget(baseDir, TAB_NAME, true, true)
            } else {
                terminalManager.createNewSession(baseDir, TAB_NAME, explicitShellCommand, true, true)
            }
            val content = markClaudeCodeTerminal(terminalManager, widget)
            content?.putUserData(CLAUDE_CODE_TERMINAL_SHELL_KEY, shellIdentity)
            if (!sendCommandToTerminal(widget, content, command)) {
                throw IllegalStateException("Failed to execute Claude Code command")
            }
            if (content != null) {
                focusClaudeCodeTerminal(terminalManager, ClaudeCodeTerminal(widget, content))
            }
        } catch (sendError: Throwable) {
            widget?.let { clearClaudeCodeMetadata(terminalManager, it) }
            throw sendError
        }
    }

    fun isClaudeCodeTerminalActive(): Boolean {
        return try {
            val terminalManager = TerminalToolWindowManager.getInstance(project)
            findDisplayedClaudeCodeTerminal(terminalManager) != null
        } catch (t: Throwable) {
            logger.warn("Failed to inspect Claude Code terminal active state", t)
            false
        }
    }

    fun typeIntoActiveClaudeCodeTerminal(text: String): Boolean {
        return try {
            val terminalManager = TerminalToolWindowManager.getInstance(project)
            val terminal = findDisplayedClaudeCodeTerminal(terminalManager) ?: return false
            typeText(terminal.widget, text)
        } catch (t: Throwable) {
            logger.warn("Failed to type into Claude Code terminal", t)
            false
        }
    }

    private fun locateClaudeCodeTerminal(manager: TerminalToolWindowManager): ClaudeCodeTerminal? = try {
        val terminals = manager.terminalWidgets.asSequence().mapNotNull { widget ->
            val content = manager.getContainer(widget)?.content ?: return@mapNotNull null
            val isClaudeCode = content.getUserData(CLAUDE_CODE_TERMINAL_KEY) == true || content.displayName == TAB_NAME
            if (!isClaudeCode) {
                return@mapNotNull null
            }
            ClaudeCodeTerminal(widget, content)
        }.toList()
        terminals.firstOrNull { it.content.getUserData(CLAUDE_CODE_TERMINAL_KEY) == true }
            ?: terminals.firstOrNull()
    } catch (t: Throwable) {
        logger.warn("Failed to inspect existing terminal widgets", t)
        null
    }

    private fun findDisplayedClaudeCodeTerminal(manager: TerminalToolWindowManager): ClaudeCodeTerminal? {
        val terminal = locateClaudeCodeTerminal(manager) ?: return null
        val toolWindow = resolveTerminalToolWindow(manager) ?: return null
        val selectedContent = toolWindow.contentManager.selectedContent ?: return null
        if (selectedContent != terminal.content || !toolWindow.isVisible) {
            return null
        }
        return terminal
    }

    private fun focusClaudeCodeTerminal(manager: TerminalToolWindowManager, terminal: ClaudeCodeTerminal) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }

            try {
                val toolWindow = resolveTerminalToolWindow(manager)
                if (toolWindow == null) {
                    logger.warn("Terminal tool window is not available for focusing Claude Code")
                    return@invokeLater
                }

                val contentManager = toolWindow.contentManager
                if (contentManager.selectedContent != terminal.content) {
                    contentManager.setSelectedContent(terminal.content, true)
                }

                toolWindow.activate({
                    runCatching { terminal.widget.requestFocus() }
                        .onFailure { logger.warn("Failed to request focus for Claude Code terminal", it) }
                }, true)
            } catch (focusError: Throwable) {
                logger.warn("Failed to focus existing Claude Code terminal", focusError)
            }
        }
    }

    private fun resolveTerminalToolWindow(manager: TerminalToolWindowManager) =
        manager.getToolWindow()
            ?: ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

    private fun markClaudeCodeTerminal(manager: TerminalToolWindowManager, widget: TerminalWidget): Content? {
        return try {
            manager.getContainer(widget)?.content?.also { content ->
                content.putUserData(CLAUDE_CODE_TERMINAL_KEY, true)
                setClaudeCodeRunning(content, false)
                ensureTerminationCallback(widget, content)
                content.displayName = TAB_NAME
            }
        } catch (t: Throwable) {
            logger.warn("Failed to tag Claude Code terminal metadata", t)
            null
        }
    }

    private fun clearClaudeCodeMetadata(manager: TerminalToolWindowManager, widget: TerminalWidget) {
        try {
            manager.getContainer(widget)?.content?.let { clearClaudeCodeMetadata(it) }
        } catch (t: Throwable) {
            logger.warn("Failed to clear Claude Code terminal metadata", t)
        }
    }

    private fun clearClaudeCodeMetadata(content: Content) {
        content.putUserData(CLAUDE_CODE_TERMINAL_KEY, null)
        content.putUserData(CLAUDE_CODE_TERMINAL_RUNNING_KEY, null)
        content.putUserData(CLAUDE_CODE_TERMINAL_CALLBACK_KEY, null)
        content.putUserData(CLAUDE_CODE_TERMINAL_SHELL_KEY, null)
    }

    private fun reuseClaudeCodeTerminal(terminal: ClaudeCodeTerminal, command: String): Boolean {
        ensureTerminationCallback(terminal.widget, terminal.content)
        return sendCommandToTerminal(terminal.widget, terminal.content, command)
    }

    private fun sendCommandToTerminal(widget: TerminalWidget, content: Content?, command: String): Boolean {
        val plan = scriptFactory.buildPlan(command) ?: return false
        return try {
            widget.sendCommandToExecute(plan.command)
            setClaudeCodeRunning(content, true)
            true
        } catch (throwable: Throwable) {
            logger.warn("Failed to execute Claude Code command", throwable)
            setClaudeCodeRunning(content, false)
            runCatching { plan.cleanupOnFailure() }
            false
        }
    }

    private fun isClaudeCodeRunning(terminal: ClaudeCodeTerminal): Boolean {
        val liveState = invokeIsCommandRunning(terminal.widget)
        if (liveState != null) {
            setClaudeCodeRunning(terminal.content, liveState)
            return liveState
        }
        return terminal.content.getUserData(CLAUDE_CODE_TERMINAL_RUNNING_KEY) ?: false
    }

    private fun setClaudeCodeRunning(content: Content?, running: Boolean) {
        content?.putUserData(CLAUDE_CODE_TERMINAL_RUNNING_KEY, running)
    }

    private fun ensureTerminationCallback(widget: TerminalWidget, content: Content?) {
        if (content == null || content.getUserData(CLAUDE_CODE_TERMINAL_CALLBACK_KEY) == true) return
        try {
            widget.addTerminationCallback({ setClaudeCodeRunning(content, false) }, content)
            content.putUserData(CLAUDE_CODE_TERMINAL_CALLBACK_KEY, true)
        } catch (t: Throwable) {
            logger.warn("Failed to register termination callback", t)
        }
    }

    private fun invokeIsCommandRunning(widget: TerminalWidget): Boolean? =
        runCatching {
            val method = widget.javaClass.methods.firstOrNull { it.name == "isCommandRunning" && it.parameterCount == 0 }
            method?.apply { isAccessible = true }?.invoke(widget) as? Boolean
        }.getOrNull()

    private fun typeText(widget: TerminalWidget, text: String): Boolean {
        val connector = runCatching { widget.ttyConnector }.getOrNull()
        if (connector != null) {
            return runCatching {
                connector.write(text)
                true
            }.getOrElse {
                logger.warn("Failed to write to Claude Code terminal connector", it)
                false
            }
        }

        val methods = widget.javaClass.methods
        val typeMethod = methods.firstOrNull { it.name == "typeText" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java }
        if (typeMethod != null) {
            return runCatching {
                typeMethod.isAccessible = true
                typeMethod.invoke(widget, text)
                true
            }.getOrElse {
                logger.warn("Failed to invoke typeText on Claude Code terminal", it)
                false
            }
        }

        val pasteMethod = methods.firstOrNull { it.name == "pasteText" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java }
        if (pasteMethod != null) {
            return runCatching {
                pasteMethod.isAccessible = true
                pasteMethod.invoke(widget, text)
                true
            }.getOrElse {
                logger.warn("Failed to invoke pasteText on Claude Code terminal", it)
                false
            }
        }

        return false
    }
}

object ShellLaunchCommandBuilder {
    fun shellIdentity(state: ClaudeCodeLauncherSettings.State): String =
        shellCommand(state)?.joinToString("\u0000") ?: "ide-default"

    fun shellCommand(state: ClaudeCodeLauncherSettings.State): List<String>? =
        when (state.launchShellMode) {
            LaunchShellMode.FOLLOW_IDE_DEFAULT -> null
            LaunchShellMode.POWERSHELL -> buildPowerShellShellCommand(state)
            LaunchShellMode.WSL -> buildWslShellCommand(state)
        }

    private fun buildPowerShellShellCommand(
        state: ClaudeCodeLauncherSettings.State,
    ): List<String> {
        val executable = state.powerShellExecutablePath.trim().ifEmpty {
            state.powerShellVersion.defaultExecutable()
        }
        return listOf(executable, "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass")
    }

    private fun buildWslShellCommand(state: ClaudeCodeLauncherSettings.State): List<String> {
        val executable = state.wslExecutablePath.trim().ifEmpty { "wsl.exe" }
        val distro = state.wslDistro.trim().ifEmpty { "Ubuntu" }
        return buildList {
            add(executable)
            if (distro.isNotEmpty()) {
                add("-d")
                add(distro)
            }
        }
    }
}
