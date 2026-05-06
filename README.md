# ClaudeCode-launcher - IntelliJ Plugin

[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-2024.2+-orange.svg)](https://www.jetbrains.com/idea/)

ClaudeCode-launcher is an unofficial IntelliJ IDEA plugin that keeps the Claude Code CLI one click away inside the IDE.

> **Important:** Install the [Claude Code CLI](https://docs.anthropic.com/en/docs/claude-code/overview) separately before using this plugin.

> **For Windows users:** Select the launch shell in _Settings (-> Other Settings) -> ClaudeCode-launcher_ when you need to force PowerShell or WSL instead of following the IDE terminal default.

## Features

- One-click launch from the toolbar or Tools menu.
- Integrated terminal that opens a dedicated "Claude Code" tab in the project root.
- Current-file and selection/class range insertion into the active Claude Code terminal.
- Claude Code completion hooks for IDE notifications, VFS refresh, and automatic opening of recently changed files.
- Claude CLI options for model, effort, permission mode, add-dir, tools, MCP config, settings, and custom arguments.
- Explicit launch shell options for following the IDE default, PowerShell, or WSL.

## Requirements

- IntelliJ IDEA 2024.2 or later, or another compatible JetBrains IDE.
- Claude Code CLI installed and available as `claude` in the selected shell PATH.

## Usage

1. Click **Launch Claude Code** in the main toolbar, or choose **Tools** -> **Launch Claude Code**.
2. The integrated terminal opens a "Claude Code" tab and runs `claude`.
3. When the Claude Code tab is active, click the toolbar action again to insert the current file path.
4. In an editor, use **Send Selection/Class to Claude Code** to insert `path:start-end ` for the current selection or enclosing class.

## Configuration

Open **Settings (-> Other Settings) -> ClaudeCode-launcher** to configure:

- Launch shell: follow IDE default, PowerShell, or WSL.
- PowerShell version and optional executable path.
- Optional WSL executable path and distro.
- Claude model, effort, permission mode, add-dir paths, tool allow/deny lists, MCP config, settings file, and custom arguments.
- Notifications and automatic file opening through Claude Code `Stop` and `StopFailure` hooks.

## Development

```bash
./gradlew test
./gradlew buildPlugin
./gradlew runIde
```

Packaged plugin zips are written to `build/distributions/`.

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
