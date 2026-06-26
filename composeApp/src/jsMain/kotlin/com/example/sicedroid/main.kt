package com.example.sicedroid

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.example.sicedroid.db.WebLocalDataSource

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(
        title = "SICEDroid",
        canvasElementId = "ComposeTarget"
    ) {
        App(localDataSource = WebLocalDataSource())
    }
}
