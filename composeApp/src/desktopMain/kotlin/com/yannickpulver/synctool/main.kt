package com.yannickpulver.synctool

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SyncTool",
    ) {
        LaunchedEffect(window.rootPane) {
            with(window.rootPane) {
                putClientProperty("apple.awt.transparentTitleBar", true)
                putClientProperty("apple.awt.fullWindowContent", true)
                putClientProperty("apple.awt.windowTitleVisible", false)
            }
        }

        val toolbarHeight = window.height - window.contentPane.height

        App(toolbarHeight)
    }
}