# Repository Guidelines

## Project Structure & Module Organization
- `src/main/kotlin/com/github/liguanyu/claudecodelauncher/` — plugin actions (`*Action`), terminal service, CLI arg builder, settings UI/options.
- `src/main/resources/META-INF/plugin.xml` — plugin metadata; icons live in `src/main/resources/icons/`.
- `src/test/kotlin/com/github/liguanyu/claudecodelauncher/` — JUnit tests mirroring main packages (e.g., `ClaudeCodeArgsBuilderTest`, `InsertPayloadResolverTest`).
- Build outputs land in `build/`; packaged zips in `build/distributions/`.

## Build, Test, and Development Commands
- `./gradlew buildPlugin` — compile and package the IntelliJ plugin zip into `build/distributions/`.
- `./gradlew runIde` — launch the sandbox IDE with the plugin for manual checks.
- `./gradlew test` — run the JUnit suite; use `--tests com.github.liguanyu.claudecodelauncher.*` to target classes.
- `./gradlew clean` — remove build outputs before a fresh run (optional).

## Coding Style & Naming Conventions
- Kotlin 2.2, JVM 21; follow standard Kotlin style (4-space indent, trailing commas where helpful, immutable vals preferred).
- Public APIs and services should include concise KDoc; logger via `com.intellij.openapi.diagnostic.logger`.
- Classes/objects use PascalCase; enums like `Model`, `PermissionMode`; actions end with `Action`; tests end with `Test`.
- Keep IntelliJ platform threading rules in mind (UI changes on EDT; services stay light).

## Testing Guidelines
- Tests live under `src/test/kotlin` and use JUnit 4 (`@Test`); mirror package paths.
- Favor fast, isolated unit tests for CLI arg building, action routing, and terminal handling.
- When adding behavior, pair with a regression test; name methods descriptively (`methodName_condition_expected`).
- For manual verification, prefer `./gradlew runIde` to exercise toolbar actions and settings UI.

## Commit & Pull Request Guidelines
- Follow recent Conventional Commit style seen in history (`feat:`, `fix:`, optional issue refs like `(#95)`); keep subject in imperative, ≤72 chars.
- Include PR summary, rationale, and testing notes; attach screenshots/GIFs for UI changes and mention affected IDE versions (targets 2024.2+).
- Update `plugin.xml` change notes when user-facing behavior shifts; align version in `build.gradle.kts` as needed.

## Security & Configuration Tips
- Keep signing/publish secrets out of the repo; set `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD` as environment variables when building for release.
- The plugin shells out to the Claude Code CLI; ensure `claude` is on PATH in the sandbox IDE and verify shell compatibility on Windows via plugin settings.
