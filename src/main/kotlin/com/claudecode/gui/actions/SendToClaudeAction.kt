package com.claudecode.gui.actions

import com.claudecode.gui.services.ClaudeCodeService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager

class SendToClaudeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val selectedText = editor.selectionModel.selectedText
        if (selectedText.isNullOrBlank()) {
            return
        }

        // Get the service
        val service = project.service<ClaudeCodeService>()

        // Open the tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Claude Code")
        toolWindow?.show()

        // Send the selected text with context
        val fileName = e.getData(CommonDataKeys.VIRTUAL_FILE)?.name ?: "unknown"
        val message = "Here's some code from $fileName:\n\n```\n$selectedText\n```\n\nCan you help me with this?"

        service.sendMessage(message)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabled = hasSelection
    }
}
