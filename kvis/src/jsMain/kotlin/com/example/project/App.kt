package com.example.project

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.fetch.RequestInit
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.json
import kotlin.js.Json
import kotlin.js.JSON

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

// DTO структуры для хранения истории шагов от бэкенда
data class VisualNode(val id: String, val label: String, val x: Int, val y: Int, val color: String)
data class VisualEdge(val id: String, val from: String, val to: String, val color: String)
data class VisualStep(val description: String, val nodes: List<VisualNode>, val edges: List<VisualEdge>)

class App : Application() {
    private var networkInstance: VisNetwork? = null
    private var currentNodes = VisDataSet(emptyArray())
    private var currentEdges = VisDataSet(emptyArray())

    private var selectedSourceNodeId: String? = null
    private var nodeCounter = 1
    private var isExecutionMode = false

    // Массив шагов алгоритма и индекс текущей позиции
    private val stepsList = mutableListOf<VisualStep>()
    private var currentStepIndex = 0

    // Ссылки на элементы UI для управления состоянием
    private lateinit var stepBackwardButton: Button
    private lateinit var stopButton: Button
    private lateinit var stepForwardButton: Button
    private lateinit var startSortButton: Button
    private lateinit var statusText: Div

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

                // 2. ОСНОВНАЯ СЕТКА (Две колонки)
                hPanel(spacing = 25, className = "justify-content-center") {

                    // --- ЛЕВАЯ КОЛОНКА: Холст графа ---
                    vPanel(spacing = 15) {
                        width = 500.px

                        div(className = "rounded p-3 text-center position-relative") {
                            setStyle("background-color", "#393d4f")
                            setStyle("height", "500.px")

                            p(className = "fw-bold mb-2 text-start") {
                                + "Создайте ваш граф"
                                setStyle("color", "#a594f9")
                            }

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

                    // --- ПРАВАЯ КОЛОНКА: Панель управления ---
                    vPanel(spacing = 20) {
                        width = 450.px

                        // Кнопки пошагового просмотра
                        hPanel(spacing = 15, className = "justify-content-between") {
                            stepBackwardButton = button("← НАЗАД", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                                onClick { navigateStep(-1) }
                            }
                            stopButton = button("СТОП ‖", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                                onClick { exitExecutionMode() }
                            }
                            stepForwardButton = button("ВПЕРЁД →", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                                onClick { navigateStep(1) }
                            }
                        }

                        // Главная кнопка старта алгоритма
                        startSortButton = button("Топологическая сортировка >>>", className = "btn w-100 p-3 fw-bold") {
                            setStyle("background-color", "#393d4f")
                            setStyle("color", "#a594f9")
                            setStyle("font-size", "1.1rem")
                            onClick { sendGraphToBackend() }
                        }

                        // Вывод результатов
                        vPanel(spacing = 10, className = "rounded p-3 flex-grow-1") {
                            setStyle("background-color", "#393d4f")
                            setStyle("min-height", "320.px")

                            p(className = "fw-bold text-start mb-2") {
                                + "Результат работы алгоритма"
                                setStyle("color", "#a594f9")
                            }

                            div(className = "p-2 rounded overflow-auto") {
                                setStyle("background-color", "#2d3142")
                                setStyle("height", "240.px")
                                statusText = div {
                                    + "Ожидание построения графа и запуска..."
                                    setStyle("color", "#a594f9")
                                    addCssClass("font-monospace")
                                    setStyle("font-size", "14px")
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

    // ОТПРАВКА ДАННЫХ НА TOR БЭКЕНД
    @OptIn(ExperimentalWasmJsInterop::class)
    private fun sendGraphToBackend() {
        statusText.content = "Подготовка структуры графа..."

        val rawNodes: Array<Json> = try {
            currentNodes.get()
        } catch (e: Exception) {
            statusText.content = "Ошибка выгрузки вершин: ${e.message}"
            return
        }

        val rawEdges: Array<Json> = try {
            currentEdges.get()
        } catch (e: Exception) {
            statusText.content = "Ошибка выгрузки рёбер: ${e.message}"
            return
        }

        if (rawNodes.isEmpty()) {
            statusText.content = "Ошибка: Невозможно запустить алгоритм на пустом графе."
            return
        }

        // Преобразование в payload-формат для сериализации бэкенда
        val verticesPayload = rawNodes.map { node ->
            json(
                "id" to node["id"].toString(),
                "x" to (node["x"]?.unsafeCast<Double>()?.toInt() ?: 0),
                "y" to (node["y"]?.unsafeCast<Double>()?.toInt() ?: 0)
            )
        }.toTypedArray()

        val edgesPayload = rawEdges.map { edge ->
            json(
                "from" to edge["from"].toString(),
                "to" to edge["to"].toString()
            )
        }.toTypedArray()

        val bodyPayload = json("vertices" to verticesPayload, "edges" to edgesPayload)

        val requestOptions = RequestInit(
            method = "POST",
            headers = json("Content-Type" to "application/json", "Accept" to "application/json"),
            body = JSON.stringify(bodyPayload)
        )

        statusText.content = "Запрос отправлен на сервер..."

        window.fetch("http://localhost:8080/api/topsort", requestOptions).then { response ->
            if (response.ok) {
                response.text().then { text ->
                    try {
                        val serverResult = JSON.parse<dynamic>(text)
                        val hasCycle = serverResult.hasCycle.unsafeCast<Boolean>()

                        if (hasCycle) {
                            statusText.content = "Ошибка: В графе есть циклическая зависимость! Топологическая сортировка невозможна."
                            exitExecutionMode()
                        } else {
                            stepsList.clear()
                            val stepsArray = serverResult.steps.unsafeCast<Array<dynamic>>()

                            for (i in 0 until stepsArray.length) {
                                val step = stepsArray[i]
                                val nodesList = mutableListOf<VisualNode>()
                                val nArr = step.nodes.unsafeCast<Array<dynamic>>()
                                for (j in 0 until nArr.length) {
                                    val n = nArr[j]
                                    nodesList.add(VisualNode(n.id.toString(), n.label.toString(), n.x.unsafeCast<Int>(), n.y.unsafeCast<Int>(), n.color.toString()))
                                }

                                val edgesList = mutableListOf<VisualEdge>()
                                val eArr = step.edges.unsafeCast<Array<dynamic>>()
                                for (k in 0 until eArr.length) {
                                    val e = eArr[k]
                                    edgesList.add(VisualEdge(e.id.toString(), e.from.toString(), e.to.toString(), e.color.toString()))
                                }
                                stepsList.add(VisualStep(step.description.toString(), nodesList, edgesList))
                            }

                            // Переводим интерфейс в режим симуляции шагов
                            isExecutionMode = true
                            currentStepIndex = 0
                            startSortButton.disabled = true
                            stopButton.disabled = false

                            updateUiForCurrentStep()
                        }
                    } catch (ex: Exception) {
                        statusText.content = "Ошибка десериализации ответа: ${ex.message}"
                    }
                }
            } else {
                statusText.content = "Ошибка сервера: ${response.status} ${response.statusText}"
            }
        }.catch { err ->
            statusText.content = "Ошибка сети: Не удалось подключиться к Ktor на порту 8080. Проверьте бэкенд."
        }
    }

    private fun navigateStep(direction: Int) {
        val newIndex = currentStepIndex + direction
        if (newIndex in 0 until stepsList.size) {
            currentStepIndex = newIndex
            updateUiForCurrentStep()
        }
    }

    // ОТРИСОВКА СОСТОЯНИЯ ТЕКУЩЕГО ШАГА НА ХОЛСТЕ
    private fun updateUiForCurrentStep() {
        if (stepsList.isEmpty()) return
        val currentStep = stepsList[currentStepIndex]

        statusText.content = currentStep.description

        currentNodes.clear()
        currentEdges.clear()

        currentStep.nodes.forEach { node ->
            currentNodes.add(json("id" to node.id, "label" to node.label, "x" to node.x, "y" to node.y, "color" to node.color))
        }

        currentStep.edges.forEach { edge ->
            currentEdges.add(json("id" to edge.id, "from" to edge.from, "to" to edge.to, "color" to json("color" to edge.color)))
        }

        stepBackwardButton.disabled = currentStepIndex == 0
        stepForwardButton.disabled = currentStepIndex == stepsList.size - 1
    }

    private fun exitExecutionMode() {
        isExecutionMode = false
        startSortButton.disabled = false
        stopButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        statusText.content = "Режим симуляции прерван. Вы можете редактировать граф."
        initInteractiveGraph()
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun initInteractiveGraph() {
        val container = document.getElementById("networkView") as? HTMLElement ?: return
        networkInstance?.destroy()

        currentNodes = VisDataSet(emptyArray())
        currentEdges = VisDataSet(emptyArray())
        nodeCounter = 1

        val data = json("nodes" to currentNodes, "edges" to currentEdges)
        val options = json(
            "physics" to json("enabled" to false),
            "edges" to json("arrows" to "to", "color" to json("color" to "#a594f9")),
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

            // 1. Приводим входящий аргумент к dynamic, чтобы pointer перестал быть Unresolved
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

            // 2. Аналогично для контекстного меню (ПКМ)
            val params = paramsJson.unsafeCast<dynamic>()

            val pointerDom = params.pointer.DOM
            val clickedNodeId = network.getNodeAt(pointerDom.unsafeCast<Json>())

            if (clickedNodeId != null) {
                val currentSource = selectedSourceNodeId
                if (currentSource == null) {
                    selectedSourceNodeId = clickedNodeId
                    currentNodes.update(json("id" to clickedNodeId, "color" to "#fea548"))
                } else {
                    val target = clickedNodeId
                    if (currentSource != target) {
                        val edgeId = "e_${currentSource}_$target"
                        currentEdges.add(json("id" to edgeId, "from" to currentSource, "to" to target))
                    }
                    resetEdgeSelection()
                }
            } else {
                resetEdgeSelection()
            }
        }
    }

    private fun resetEdgeSelection() {
        val currentSource = selectedSourceNodeId
        if (currentSource != null) {
            currentNodes.update(json("id" to currentSource, "color" to "#5bc0de"))
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