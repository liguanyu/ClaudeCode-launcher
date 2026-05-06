# IntelliJ Terminal API Notes

This project needs explicit terminal shell creation so Rider's default terminal shell does not override ClaudeCode-launcher's PowerShell/WSL setting.

## Relevant Classes

- `org.jetbrains.plugins.terminal.TerminalToolWindowManager`
- `org.jetbrains.plugins.terminal.ShellStartupOptions`
- `org.jetbrains.plugins.terminal.ShellStartupOptions.Builder`
- `com.intellij.terminal.ui.TerminalWidget`

These classes are provided by the bundled JetBrains Terminal plugin.

In a Gradle IntelliJ Platform cache, the terminal jar is usually under a path like:

```text
C:\Users\<user>\.gradle\caches\<gradle-version>\transforms\...\idea-<ide-version>-win\plugins\terminal\lib\terminal.jar
```

## Public Creation APIs Observed

`TerminalToolWindowManager` exposes:

```text
createShellWidget(String workingDirectory, String tabName, boolean requestFocus, boolean deferSessionStart): TerminalWidget

createNewSession(
    String workingDirectory,
    String tabName,
    List<String> shellCommand,
    boolean requestFocus,
    boolean deferSessionStart
): TerminalWidget

createLocalShellWidget(String workingDirectory, String tabName): ShellTerminalWidget
createLocalShellWidget(String workingDirectory, String tabName, boolean requestFocus): ShellTerminalWidget
createLocalShellWidget(String workingDirectory, String tabName, boolean requestFocus, boolean deferSessionStart): ShellTerminalWidget
```

## Important Behavior

`createShellWidget(...)` follows the IDE/Rider default terminal shell. If Rider's terminal default is WSL, calling `createShellWidget(...)` opens a WSL tab. Sending a command like:

```text
powershell.exe -NoLogo -NoExit -ExecutionPolicy Bypass -Command "..."
```

then runs PowerShell from inside the WSL terminal. That is not an explicit PowerShell terminal tab.

To force a shell, use:

```kotlin
terminalManager.createNewSession(
    workingDirectory,
    tabName,
    shellCommand,
    true,
    true,
)
```

Where `shellCommand` is a tokenized command list, not one quoted command string.

Examples:

```kotlin
listOf("powershell.exe", "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass")
```

```kotlin
listOf("pwsh.exe", "-NoLogo", "-NoExit", "-ExecutionPolicy", "Bypass")
```

```kotlin
listOf("wsl.exe", "-d", "Ubuntu")
```

The launcher defaults to `Ubuntu` when WSL is selected and the distro field is empty.

After the terminal tab is created with the explicit shell, send the Claude command through `TerminalWidget.sendCommandToExecute(...)`.

## Avoiding Stale Tab Reuse

When the configured launch shell changes, do not reuse a previous Claude Code terminal tab that was created with a different shell. Store a shell identity on the tab content and create a new tab if the identity no longer matches.

For this project:

```kotlin
Key.create<String>("claudecode.launcher.terminal.shell")
```

The identity can be derived from the explicit shell command list, or `"ide-default"` for Follow IDE Default.

## Inspection Commands

If `jar` is available:

```bash
jar tf path/to/terminal.jar | rg "TerminalToolWindowManager|ShellStartupOptions"
```

If `unzip` is available:

```bash
unzip -l path/to/terminal.jar | rg "TerminalToolWindowManager|ShellStartupOptions"
```

To inspect signatures with `javap`:

```bash
javap -classpath path/to/terminal.jar \
  org.jetbrains.plugins.terminal.TerminalToolWindowManager \
  org.jetbrains.plugins.terminal.ShellStartupOptions \
  'org.jetbrains.plugins.terminal.ShellStartupOptions$Builder'
```
