package com.elix.assistant.ai

object CommandParser {
    data class Command(val rawText: String)

    fun parse(text: String): Command? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return null
    }
}

