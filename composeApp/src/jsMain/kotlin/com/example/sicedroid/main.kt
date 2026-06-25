package com.example.sicedroid

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.example.sicedroid.db.WebLocalDataSource
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow(
            title = "SICEDroid",
            canvasElementId = "ComposeTarget"
        ) {
            App(localDataSource = WebLocalDataSource())
        }
    }
}
