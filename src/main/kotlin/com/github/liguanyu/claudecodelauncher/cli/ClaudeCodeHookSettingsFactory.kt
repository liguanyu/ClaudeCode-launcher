package com.github.liguanyu.claudecodelauncher.cli

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object ClaudeCodeHookSettingsFactory {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun create(port: Int): Path {
        val settingsPath = Files.createTempFile("claude-code-launcher-settings-", ".json")
        val url = "http://localhost:$port/refresh"
        Files.writeString(settingsPath, gson.toJson(buildSettings(url)), StandardCharsets.UTF_8)
        settingsPath.toFile().deleteOnExit()
        return settingsPath
    }

    private fun buildSettings(url: String): JsonObject =
        JsonObject().apply {
            add("hooks", JsonObject().apply {
                add("Stop", hookArray(url))
                add("StopFailure", hookArray(url))
            })
        }

    private fun hookArray(url: String): JsonArray =
        JsonArray().apply {
            add(JsonObject().apply {
                add("hooks", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "http")
                        addProperty("url", url)
                    })
                })
            })
        }
}
