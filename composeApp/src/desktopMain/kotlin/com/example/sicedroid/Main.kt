package com.example.sicedroid

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.sicedroid.db.DesktopDriverFactory
import com.example.sicedroid.db.SqlDelightLocalDataSource

fun main() = application {
    val dataSource = SqlDelightLocalDataSource(DesktopDriverFactory())
    Window(
        onCloseRequest = ::exitApplication,
        title = "SICEDroid",
    ) {
        App(localDataSource = dataSource)
    }
}
