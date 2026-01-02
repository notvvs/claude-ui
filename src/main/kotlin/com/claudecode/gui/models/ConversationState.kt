package com.claudecode.gui.models

data class ConversationState(
    val messages: MutableList<Message> = mutableListOf(),
    val isProcessing: Boolean = false,
    val currentWorkingDirectory: String = ""
)
