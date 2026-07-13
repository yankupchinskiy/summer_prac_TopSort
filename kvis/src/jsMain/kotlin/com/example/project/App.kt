package com.example.project

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.RequestInit
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.json
import kotlin.js.Json
import kotlin.js.JSON
import kotlin.math.ceil
import kotlin.math.sqrt

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

private const val maxVertexIdLength = 50

// DTO структуры для хранения истории шагов от бэкенда
data class VisualNode(val id: String, val label: String, val x: Int, val y: Int, val color: String)
data class VisualEdge(val id: String, val from: String, val to: String, val color: String)
data class VisualStep(val description: String, val nodes: List<VisualNode>, val edges: List<VisualEdge>)
data class ImportedVertex(val id: String, val x: Int?, val y: Int?)
data class ImportedEdge(val from: String, val to: String)
data class GraphPosition(val x: Int, val y: Int)

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
                            onClick { openGraphFilePicker() }
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

    private fun openGraphFilePicker() {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = ".json,application/json"

        input.onchange = {
            val file = input.files?.item(0)
            if (file != null) {
                val reader = js("new FileReader()")
                reader.onload = {
                    val content = reader.result?.toString()
                    if (content == null) {
                        statusText.content = "Ошибка: файл не удалось прочитать."
                    } else {
                        loadGraphFromJson(content)
                    }
                }
                reader.onerror = {
                    statusText.content = "Ошибка чтения файла."
                }
                reader.readAsText(file)
            }
        }

        input.click()
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun loadGraphFromJson(content: String) {
        try {
            val payload = JSON.parse<dynamic>(content)
            val verticesArray = payload.vertices
            val edgesArray = payload.edges

            if (!isArray(verticesArray)) {
                throw IllegalArgumentException("Поле vertices должно быть массивом.")
            }
            if (!isArray(edgesArray)) {
                throw IllegalArgumentException("Поле edges должно быть массивом.")
            }

            val importedVertices = parseImportedVertices(verticesArray)
            val importedEdges = parseImportedEdges(edgesArray)
            applyImportedGraph(importedVertices, importedEdges)
        } catch (e: Exception) {
            statusText.content = "Ошибка загрузки JSON: ${e.message}"
        }
    }

    private fun parseImportedVertices(verticesArray: dynamic): List<ImportedVertex> {
        if (verticesArray.length == 0) {
            throw IllegalArgumentException("Граф должен содержать хотя бы одну вершину.")
        }

        val ids = mutableSetOf<String>()
        val vertices = mutableListOf<ImportedVertex>()

        for (index in 0 until verticesArray.length) {
            val item = verticesArray[index]
            val id = if (jsTypeOf(item) == "string") {
                item.toString()
            } else {
                readRequiredString(item.id, "Vertex at index $index id")
            }

            requireValidVertexId(id, "Vertex at index $index")
            if (!ids.add(id)) {
                throw IllegalArgumentException("JSON содержит повторяющийся id вершины '$id'.")
            }

            val x = if (jsTypeOf(item) == "string") null else readOptionalInt(item.x)
            val y = if (jsTypeOf(item) == "string") null else readOptionalInt(item.y)
            vertices.add(ImportedVertex(id, x, y))
        }

        return vertices
    }

    private fun parseImportedEdges(edgesArray: dynamic): List<ImportedEdge> {
        val edges = mutableListOf<ImportedEdge>()

        for (index in 0 until edgesArray.length) {
            val item = edgesArray[index]
            val from: String
            val to: String

            if (isArray(item)) {
                if (item.length != 2) {
                    throw IllegalArgumentException("Edge at index $index должен содержать ровно две вершины.")
                }
                from = readRequiredString(item[0], "Edge at index $index source")
                to = readRequiredString(item[1], "Edge at index $index target")
            } else {
                from = readRequiredString(item.from, "Edge at index $index source")
                to = readRequiredString(item.to, "Edge at index $index target")
            }

            requireValidVertexId(from, "Edge at index $index source")
            requireValidVertexId(to, "Edge at index $index target")
            edges.add(ImportedEdge(from, to))
        }

        return edges
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun applyImportedGraph(vertices: List<ImportedVertex>, edges: List<ImportedEdge>) {
        val vertexIds = vertices.map { it.id }.toSet()
        edges.forEachIndexed { index, edge ->
            if (!vertexIds.contains(edge.from)) {
                throw IllegalArgumentException("Edge at index $index ссылается на неизвестную вершину '${edge.from}'.")
            }
            if (!vertexIds.contains(edge.to)) {
                throw IllegalArgumentException("Edge at index $index ссылается на неизвестную вершину '${edge.to}'.")
            }
        }

        isExecutionMode = false
        stepsList.clear()
        currentStepIndex = 0
        startSortButton.disabled = false
        stopButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        resetEdgeSelection()

        currentNodes.clear()
        currentEdges.clear()

        val centeredPositions = buildCenteredPositions(vertices)
        vertices.forEachIndexed { index, vertex ->
            val position = centeredPositions[index]

            currentNodes.add(json(
                "id" to vertex.id,
                "label" to vertex.id,
                "x" to position.x,
                "y" to position.y,
                "color" to "#5bc0de"
            ))
        }

        edges.forEachIndexed { index, edge ->
            currentEdges.add(json(
                "id" to "e_${edge.from}_${edge.to}_$index",
                "from" to edge.from,
                "to" to edge.to
            ))
        }

        nodeCounter = calculateNextNodeCounter(vertices.map { it.id })
        statusText.content = "Граф загружен из файла: ${vertices.size} вершин, ${edges.size} рёбер."
        fitGraphToCanvas()
    }

    private fun buildCenteredPositions(vertices: List<ImportedVertex>): List<GraphPosition> {
        val columns = maxOf(1, ceil(sqrt(vertices.size.toDouble())).toInt())
        val positions = vertices.mapIndexed { index, vertex ->
            val row = index / columns
            val column = index % columns
            GraphPosition(
                x = vertex.x ?: (column * 180),
                y = vertex.y ?: (row * 120)
            )
        }

        val minX = positions.minOf { it.x }
        val maxX = positions.maxOf { it.x }
        val minY = positions.minOf { it.y }
        val maxY = positions.maxOf { it.y }
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2

        return positions.map { position ->
            GraphPosition(
                x = position.x - centerX,
                y = position.y - centerY
            )
        }
    }

    private fun fitGraphToCanvas() {
        window.setTimeout({
            networkInstance?.fit(json(
                "animation" to json(
                    "duration" to 250,
                    "easingFunction" to "easeInOutQuad"
                )
            ))
        }, 50)
    }

    private fun requireValidVertexId(id: String, owner: String) {
        if (id.isBlank()) {
            throw IllegalArgumentException("$owner is empty.")
        }
        if (id.length > maxVertexIdLength) {
            throw IllegalArgumentException("$owner exceeds $maxVertexIdLength characters.")
        }
    }

    private fun readRequiredString(value: dynamic, owner: String): String {
        if (value == null || jsTypeOf(value) == "undefined") {
            throw IllegalArgumentException("$owner is missing.")
        }
        return value.toString()
    }

    private fun readOptionalInt(value: dynamic): Int? {
        if (value == null || jsTypeOf(value) == "undefined") {
            return null
        }
        val number = js("Number(value)").unsafeCast<Double>()
        return if (js("Number.isFinite(number)").unsafeCast<Boolean>()) number.toInt() else null
    }

    private fun isArray(value: dynamic): Boolean = js("Array.isArray(value)").unsafeCast<Boolean>()

    private fun calculateNextNodeCounter(ids: List<String>): Int {
        val maxNumericId = ids.mapNotNull { it.toIntOrNull() }.maxOrNull()
        return (maxNumericId ?: 0) + 1
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
                            resetExecutionControls()
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
        resetExecutionControls()
        statusText.content = "Режим симуляции прерван. Вы можете редактировать граф."
        initInteractiveGraph()
    }

    private fun resetExecutionControls() {
        isExecutionMode = false
        stepsList.clear()
        currentStepIndex = 0
        startSortButton.disabled = false
        stopButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
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