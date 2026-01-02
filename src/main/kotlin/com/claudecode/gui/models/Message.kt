package com.claudecode.gui.models

import java.time.LocalDateTime

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val isError: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
