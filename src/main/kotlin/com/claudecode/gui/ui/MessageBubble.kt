package com.claudecode.gui.ui

import com.claudecode.gui.models.Message
import com.claudecode.gui.models.MessageRole
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.text.SimpleDateFormat
import javax.swing.*

class MessageBubble(private val message: Message) : JPanel(BorderLayout()) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss")

    init {
        setupUI()
    }

    private fun setupUI() {
        val backgroundColor = when (message.role) {
            MessageRole.USER -> JBColor(Color(230, 240, 255), Color(40, 50, 70))
            MessageRole.ASSISTANT -> JBColor(Color(240, 255, 240), Color(50, 60, 50))
            MessageRole.SYSTEM -> if (message.isError) {
                JBColor(Color(255, 230, 230), Color(70, 40, 40))
            } else {
                JBColor(Color(245, 245, 245), Color(60, 60, 60))
            }
        }

        background = backgroundColor
        border = JBUI.Borders.compound(
            JBUI.Borders.empty(5),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.GRAY, 1, true),
                JBUI.Borders.empty(10)
            )
        )

        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

        // Header with role and timestamp
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = backgroundColor

            val roleLabel = JLabel(getRoleLabel()).apply {
                font = font.deriveFont(font.style or java.awt.Font.BOLD)
            }

            val timeLabel = JLabel(timeFormat.format(java.util.Date.from(
                message.timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant()
            ))).apply {
                foreground = JBColor.GRAY
                font = font.deriveFont(10f)
            }

            add(roleLabel)
            add(Box.createHorizontalGlue())
            add(timeLabel)
        }

        add(header, BorderLayout.NORTH)

        // Message content
        val contentArea = JTextArea(message.content).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = backgroundColor
            border = JBUI.Borders.empty(5, 0, 0, 0)
            font = font.deriveFont(13f)
        }

        add(contentArea, BorderLayout.CENTER)
    }

    private fun getRoleLabel(): String = when (message.role) {
        MessageRole.USER -> "You"
        MessageRole.ASSISTANT -> "Claude Code"
        MessageRole.SYSTEM -> "System"
    }
}
