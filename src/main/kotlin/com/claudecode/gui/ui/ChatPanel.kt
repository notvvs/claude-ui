package com.claudecode.gui.ui

import com.claudecode.gui.models.Message
import com.claudecode.gui.models.MessageRole
import com.claudecode.gui.services.ClaudeCodeService
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

class ChatPanel(private val project: Project, private val service: ClaudeCodeService) : JPanel(BorderLayout()) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val chatArea = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color.WHITE
    }

    private val scrollPane = JBScrollPane(chatArea).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        border = JBUI.Borders.empty()
    }

    private val inputArea = JTextArea(3, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(5)
    }

    private val sendButton = JButton("Send").apply {
        addActionListener {
            sendMessage()
        }
    }

    private val clearButton = JButton("Clear").apply {
        addActionListener {
            service.clearMessages()
            chatArea.removeAll()
            chatArea.revalidate()
            chatArea.repaint()
        }
    }

    init {
        setupUI()
        observeMessages()

        // Start Claude session with project base path
        service.startClaudeSession(project.basePath ?: System.getProperty("user.dir"))
    }

    private fun setupUI() {
        // Top panel with messages
        add(scrollPane, BorderLayout.CENTER)

        // Bottom panel with input
        val inputPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5)

            val inputScrollPane = JBScrollPane(inputArea).apply {
                preferredSize = Dimension(0, 80)
                border = JBUI.Borders.compound(
                    JBUI.Borders.empty(5),
                    BorderFactory.createLineBorder(Color.GRAY)
                )
            }

            add(inputScrollPane, BorderLayout.CENTER)

            val buttonPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(Box.createHorizontalGlue())
                add(clearButton)
                add(Box.createHorizontalStrut(5))
                add(sendButton)
            }

            add(buttonPanel, BorderLayout.SOUTH)
        }

        add(inputPanel, BorderLayout.SOUTH)

        // Add Enter key binding
        inputArea.inputMap.put(
            KeyStroke.getKeyStroke("control ENTER"),
            "send"
        )
        inputArea.actionMap.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                sendMessage()
            }
        })
    }

    private fun sendMessage() {
        val text = inputArea.text.trim()
        if (text.isNotEmpty()) {
            service.sendMessage(text)
            inputArea.text = ""
        }
    }

    private fun observeMessages() {
        scope.launch {
            service.messages.collectLatest { messages ->
                updateChatArea(messages)
            }
        }
    }

    private fun updateChatArea(messages: List<Message>) {
        SwingUtilities.invokeLater {
            chatArea.removeAll()

            messages.forEach { message ->
                chatArea.add(MessageBubble(message))
                chatArea.add(Box.createVerticalStrut(10))
            }

            chatArea.revalidate()
            chatArea.repaint()

            // Scroll to bottom
            SwingUtilities.invokeLater {
                val scrollBar = scrollPane.verticalScrollBar
                scrollBar.value = scrollBar.maximum
            }
        }
    }

    fun dispose() {
        scope.cancel()
        service.stopSession()
    }
}
