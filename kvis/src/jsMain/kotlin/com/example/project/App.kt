package com.example.project

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.js.json
import kotlin.js.Json

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.Hot
import io.kvision.html.div
import io.kvision.html.h1
import io.kvision.html.p
import io.kvision.html.button
import io.kvision.html.Button
import io.kvision.html.Div
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.panel.hPanel
import io.kvision.startApplication
import io.kvision.utils.px
import io.kvision.utils.perc
import io.kvision.core.Widget

class App : Application() {
    private var networkInstance: dynamic = null
    private var currentNodes = VisDataSet(emptyArray())
    private var currentEdges = VisDataSet(emptyArray())

    private var selectedSourceNodeId: String? = null
    private var nodeCounter = 1
    private var isExecutionMode = false

    // Ссылки на элементы UI для управления состоянием
    private lateinit var stepBackwardButton: Button
    private lateinit var stopButton: Button
    private lateinit var stepForwardButton: Button
    private lateinit var startSortButton: Button
    private lateinit var logContainer: Div

    override fun start() {
        root("kvapp") {
            vPanel(spacing = 20) {
                padding = 20.px
                setStyle("background-color", "#232733")
                setStyle("min-height", "100vh")

                // 1. ТОП ЗАГОЛОВОК
                div(className = "text-center p-3 rounded shadow-sm") {
                    setStyle("background-color", "#393d4f")
                    h1 {
                        + "ТОПОЛОГИЧЕСКАЯ СОРТИРОВКА АЛГОРИТМОМ КАНА"
                        addCssClass("h5")
                        addCssClass("text-uppercase")
                        addCssClass("fw-bold")
                        addCssClass("m-0")
                        setStyle("color", "#a594f9")
                    }
                }

                // 2. ОСНОВНАЯ СЕТКА (Две колонки согласно прототипу)
                hPanel(spacing = 25, className = "justify-content-center") {

                    // --- ЛЕВАЯ КОЛОНКА: Холст графа ---
                    vPanel(spacing = 15) {
                        width = 500.px

                        vPanel(className = "rounded p-3 text-center position-relative") {
                            setStyle("background-color", "#393d4f")
                            setStyle("height", "500.px")

                            p {
                                + "Создайте ваш граф"
                                addCssClass("fw-bold")
                                addCssClass("mb-2")
                                addCssClass("text-start")
                                setStyle("color", "#a594f9")
                            }

                            // Интерактивная зона для Vis.js
                            val graphContainer = Widget().apply {
                                width = 100.perc
                                height = 420.px
                                id = "networkView"
                                setStyle("background-color", "#2d3142")
                                setStyle("border", "2px dashed #4f546b")
                            }
                            add(graphContainer)
                        }

                        button("Загрузка из файла", className = "btn w-100 p-3 fw-bold") {
                            setStyle("background-color", "#393d4f")
                            setStyle("color", "#a594f9")
                            setStyle("border", "none")
                        }
                    }

                    // --- ПРАВАЯ КОЛОНКА: Панель управления и Вывод результатов ---
                    vPanel(spacing = 20) {
                        width = 450.px

                        // Блок кнопок дебага: Назад | Стоп | Вперед
                        hPanel(spacing = 15, className = "justify-content-between") {
                            stepBackwardButton = button("← НАЗАД", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                            }
                            stopButton = button("СТОП ‖", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                            }
                            stepForwardButton = button("ВПЕРЁД →", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                            }
                        }

                        // Главная кнопка старта
                        startSortButton = button("Топологическая сортировка >>>", className = "btn w-100 p-3 fw-bold") {
                            setStyle("background-color", "#393d4f")
                            setStyle("color", "#a594f9")
                            setStyle("font-size", "1.1rem")
                        }

                        // Панель логов и результатов работы
                        vPanel(spacing = 10, className = "rounded p-3 flex-grow-1") {
                            setStyle("background-color", "#393d4f")
                            setStyle("min-height", "320.px")

                            p {
                                + "Результат работы алгоритма"
                                addCssClass("fw-bold")
                                addCssClass("text-start")
                                addCssClass("mb-2")
                                setStyle("color", "#a594f9")
                            }

                            // Контейнер текстовых логов
                            logContainer = div(className = "p-2 rounded overflow-auto") {
                                setStyle("background-color", "#2d3142")
                                setStyle("height", "240.px")
                                p {
                                    + "Ожидание построения графа и запуска..."
                                    setStyle("color", "#a594f9")

                                    setStyle("font-size", "14px")                  // Размер
                                    setStyle("font-family", "Courier New, monospace") // Моноширинный шрифт для логов
                                    setStyle("font-weight", "bold")
                                }
                            }
                        }
                    }
                }
            }
        }

        window.setTimeout({
            initInteractiveGraph()
        }, 50)
    }

    private fun initInteractiveGraph() {
        val container = document.getElementById("networkView") as? HTMLElement ?: return
        if (networkInstance != null) {
            networkInstance.destroy()
        }

        currentNodes = VisDataSet(emptyArray())
        currentEdges = VisDataSet(emptyArray())
        nodeCounter = 1

        val data = json("nodes" to currentNodes, "edges" to currentEdges)
        val options = json(
            "physics" to json("enabled" to false),
            "edges" to json(
                "arrows" to "to",
                "color" to json("color" to "#a594f9")
            ),
            "interaction" to json("dragNodes" to true, "dragView" to false, "zoomView" to false),
            "manipulation" to json("enabled" to false)
        )

        val network = VisNetwork(container, data, options)
        networkInstance = network

        container.oncontextmenu = { event ->
            event.preventDefault()
            false
        }

        network.on("click") { paramsJson ->
            if (isExecutionMode) return@on

            val params = paramsJson.unsafeCast<dynamic>()
            val pointerDom = params.pointer.DOM
            val clickedNodeId = network.getNodeAt(pointerDom.unsafeCast<Json>())

            if (clickedNodeId == null) {
                resetEdgeSelection()
                val canvasX = params.pointer.canvas.x
                val canvasY = params.pointer.canvas.y
                val newNodeId = "$nodeCounter"

                currentNodes.add(json(
                    "id" to newNodeId,
                    "label" to newNodeId,
                    "x" to canvasX,
                    "y" to canvasY,
                    "color" to "#5bc0de"
                ))
                nodeCounter++
            } else {
                resetEdgeSelection()
            }
        }

        network.on("oncontext") { paramsJson ->
            if (isExecutionMode) return@on

            val params = paramsJson.unsafeCast<dynamic>()
            val pointerDom = params.pointer.DOM
            val clickedNodeId = network.getNodeAt(pointerDom.unsafeCast<Json>())

            if (clickedNodeId != null) {
                if (selectedSourceNodeId == null) {
                    selectedSourceNodeId = clickedNodeId
                    currentNodes.update(json("id" to clickedNodeId, "color" to "#fea548"))
                } else {
                    val source = selectedSourceNodeId!!
                    val target = clickedNodeId

                    if (source != target) {
                        val edgeId = "e_${source}_$target"
                        currentEdges.add(json("id" to edgeId, "from" to source, "to" to target))
                    }
                    resetEdgeSelection()
                }
            } else {
                resetEdgeSelection()
            }
        }
    }

    private fun resetEdgeSelection() {
        if (selectedSourceNodeId != null) {
            currentNodes.update(json("id" to selectedSourceNodeId!!, "color" to "#5bc0de"))
            selectedSourceNodeId = null
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