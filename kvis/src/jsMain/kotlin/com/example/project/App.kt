package com.example.project

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.Hot
import io.kvision.html.div
import io.kvision.panel.root
import io.kvision.startApplication

class App : Application() {
    override fun start() {
        root("kvapp") {
            div("Hello to firsov")
            // TODO
        }
    }
}

fun main() {
    startApplication(
        ::App,
        js("import.meta.webpackHot").unsafeCast<Hot?>(),
        BootstrapModule,
        BootstrapCssModule,
        CoreModule
    )
}
