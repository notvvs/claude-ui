package com.claudecode.gui.services

import com.claudecode.gui.models.Message
import com.claudecode.gui.models.MessageRole
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Service
class ClaudeCodeService {
    private val logger = Logger.getInstance(ClaudeCodeService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var claudeProcess: Process? = null
    private var processWriter: BufferedWriter? = null
    private var processReader: BufferedReader? = null

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun startClaudeSession(workingDirectory: String) {
        scope.launch {
            try {
                val processBuilder = ProcessBuilder(
                    "claude",
                    "--no-color"
                ).apply {
                    directory(java.io.File(workingDirectory))
                    redirectErrorStream(true)
                }

                claudeProcess = processBuilder.start()
                processWriter = BufferedWriter(OutputStreamWriter(claudeProcess!!.outputStream))
                processReader = BufferedReader(InputStreamReader(claudeProcess!!.inputStream))

                addSystemMessage("Claude Code session started in: $workingDirectory")

                // Start reading output
                launch {
                    readOutput()
                }

            } catch (e: Exception) {
                logger.error("Failed to start Claude Code session", e)
                addSystemMessage("Error starting Claude Code: ${e.message}", isError = true)
            }
        }
    }

    fun sendMessage(content: String) {
        scope.launch {
            try {
                _isProcessing.value = true

                val userMessage = Message(
                    content = content,
                    role = MessageRole.USER
                )
                addMessage(userMessage)

                processWriter?.apply {
                    write(content)
                    newLine()
                    flush()
                }

            } catch (e: Exception) {
                logger.error("Failed to send message", e)
                addSystemMessage("Error sending message: ${e.message}", isError = true)
                _isProcessing.value = false
            }
        }
    }

    private suspend fun readOutput() {
        try {
            val buffer = StringBuilder()

            while (true) {
                val charInt = processReader?.read() ?: break
                if (charInt == -1) break

                val c = charInt.toChar()

                if (c == '\n') {
                    val line = buffer.toString()
                    if (line.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            addAssistantMessage(line)
                        }
                    }
                    buffer.clear()
                } else {
                    buffer.append(c)
                }
            }

            if (buffer.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    addAssistantMessage(buffer.toString())
                }
            }

        } catch (e: Exception) {
            logger.error("Error reading output", e)
            addSystemMessage("Error reading Claude output: ${e.message}", isError = true)
        } finally {
            _isProcessing.value = false
        }
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun addAssistantMessage(content: String) {
        val message = Message(
            content = content,
            role = MessageRole.ASSISTANT
        )
        addMessage(message)
        _isProcessing.value = false
    }

    private fun addSystemMessage(content: String, isError: Boolean = false) {
        val message = Message(
            content = content,
            role = MessageRole.SYSTEM,
            isError = isError
        )
        addMessage(message)
    }

    fun stopSession() {
        try {
            processWriter?.close()
            processReader?.close()
            claudeProcess?.destroy()
            claudeProcess?.waitFor()
        } catch (e: Exception) {
            logger.error("Error stopping session", e)
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
