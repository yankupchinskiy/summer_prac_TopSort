package com.example.project

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
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
private const val colorIdle = "#8b90a3"
private const val colorActive = "#7bd88f"
private const val colorProcessed = "#5bc0de"
private const val colorCycle = "#f07178"
private const val colorEdge = "#a594f9"
private const val colorRemovedEdge = "#4f546b"
private const val colorSelectionBorder = "#fea548"

// Простые структуры для хранения графа, результата алгоритма и истории визуальных шагов.
data class VisualNode(val id: String, val label: String, val x: Int, val y: Int, val color: String)
data class VisualEdge(
    val id: String,
    val from: String,
    val to: String,
    val color: String,
    val label: String,
    val isRemoved: Boolean,
    val width: Int
)
data class VisualStep(val description: String, val nodes: List<VisualNode>, val edges: List<VisualEdge>)
data class ImportedVertex(val id: String, val x: Int?, val y: Int?)
data class ImportedEdge(val from: String, val to: String)
data class GraphPosition(val x: Int, val y: Int)
data class AlgorithmResult(val hasCycle: Boolean, val finalOrder: List<String>, val steps: List<VisualStep>)

class App : Application() {
    // Vis-network хранит вершины и рёбра отдельно, поэтому держим две коллекции.
    private var networkInstance: VisNetwork? = null
    private var currentNodes = VisDataSet(emptyArray())
    private var currentEdges = VisDataSet(emptyArray())

    // Состояние режима построения графа: выбранная вершина нужна для добавления ребра через ПКМ.
    private var selectedSourceNodeId: String? = null
    private var nodeCounter = 1
    private var isExecutionMode = false
    private var isDeleteVertexMode = false

    // Шаги нужны для автоматического показа и для кнопок назад/вперёд.
    private val stepsList = mutableListOf<VisualStep>()
    private var currentStepIndex = 0
    private var autoRunTimerId: Int? = null
    private var lastAlgorithmResult: AlgorithmResult? = null
    private var isResetAvailable = false

    // Ссылки на элементы UI нужны, чтобы менять доступность кнопок после действий пользователя.
    private lateinit var stepBackwardButton: Button
    private lateinit var stopButton: Button
    private lateinit var stepForwardButton: Button
    private lateinit var startSortButton: Button
    private lateinit var saveResultButton: Button
    private lateinit var deleteVertexButton: Button
    private lateinit var statusText: Div
    private lateinit var finalAnswerText: Div
    private lateinit var editStatusText: Div

    override fun start() {
        root("kvapp") {
            vPanel(spacing = 20) {
                padding = 20.px
                setStyle("background-color", "#232733")
                setStyle("min-height", "100vh")

                // Верхняя панель с названием работы и информацией о разработчиках
                div(className = "text-center p-3 rounded shadow-sm") {
                    setStyle("background-color", "#393d4f")
                    hPanel(className = "align-items-center justify-content-between") {
                        h1 {
                            + "ТОПОЛОГИЧЕСКАЯ СОРТИРОВКА АЛГОРИТМОМ КАНА"
                            addCssClass("h5")
                            addCssClass("text-uppercase")
                            addCssClass("fw-bold")
                            addCssClass("m-0")
                            setStyle("color", "#a594f9")
                        }
                        button("О разработчиках", className = "btn btn-sm fw-bold") {
                            setStyle("background-color", "#2d3142")
                            setStyle("color", "#a594f9")
                            setStyle("border", "1px solid #4f546b")
                            onClick { showAboutDevelopers() }
                        }
                    }
                }

                // Основная область делится на холст графа и панель управления
                hPanel(spacing = 25, className = "justify-content-center") {

                    // Здесь пользователь строит граф мышкой или загружает его из JSON
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

                        vPanel(spacing = 8, className = "rounded p-3") {
                            setStyle("background-color", "#393d4f")
                            p(className = "fw-bold mb-1 text-start") {
                                + "Редактирование графа"
                                setStyle("color", "#a594f9")
                            }
                            deleteVertexButton = button("Удалить вершину", className = "btn w-100 p-2 fw-bold") {
                                setStyle("background-color", "#2d3142")
                                setStyle("color", "#a594f9")
                                setStyle("border", "1px solid #4f546b")
                                onClick { toggleDeleteVertexMode() }
                            }
                            editStatusText = div {
                                + "ЛКМ по пустому месту - добавить вершину. ПКМ по двум вершинам - добавить ребро."
                                setStyle("color", "#a594f9")
                                setStyle("font-size", "13px")
                            }
                        }
                    }

                    // Здесь находятся запуск алгоритма, навигация по шагам и вывод ответа
                    vPanel(spacing = 20) {
                        width = 450.px

                        // Эти кнопки работают только после запуска алгоритма
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
                            onClick { pauseAutoRun("Автоматическое выполнение остановлено. Можно сохранить результат или сбросить граф.", allowReset = true) }
                            }
                            stepForwardButton = button("ВПЕРЁД →", className = "btn flex-grow-1 p-2 fw-bold") {
                                setStyle("background-color", "#393d4f")
                                setStyle("color", "#a594f9")
                                disabled = true
                                onClick { navigateStep(1) }
                            }
                        }

                        // Одна и та же кнопка запускает алгоритм, а после остановки сбрасывает граф
                        startSortButton = button("Топологическая сортировка >>>", className = "btn w-100 p-3 fw-bold") {
                            setStyle("background-color", "#393d4f")
                            setStyle("color", "#a594f9")
                            setStyle("font-size", "1.1rem")
                            onClick {
                                if (isResetAvailable) {
                                    resetApplicationState()
                                } else {
                                    startTopologicalSort()
                                }
                            }
                        }

                        saveResultButton = button("Сохранить результат в файл", className = "btn w-100 p-2 fw-bold") {
                            setStyle("background-color", "#393d4f")
                            setStyle("color", "#a594f9")
                            setStyle("border", "1px solid #4f546b")
                            disabled = true
                            onClick { saveResultToFile() }
                        }

                        // В этом блоке показывается текущий шаг и итоговый порядок вершин
                        vPanel(spacing = 10, className = "rounded p-3 flex-grow-1") {
                            setStyle("background-color", "#393d4f")
                            setStyle("min-height", "320.px")

                            p(className = "fw-bold text-start mb-2") {
                                + "Результат работы алгоритма"
                                setStyle("color", "#a594f9")
                            }

                            div(className = "p-2 rounded overflow-auto") {
                                setStyle("background-color", "#2d3142")
                                setStyle("height", "170.px")
                                statusText = div {
                                    + "Ожидание построения графа и запуска..."
                                    setStyle("color", "#a594f9")
                                    addCssClass("font-monospace")
                                    setStyle("font-size", "14px")
                                }
                            }

                            p(className = "fw-bold text-start mt-2 mb-1") {
                                + "Финальный ответ"
                                setStyle("color", "#a594f9")
                            }

                            div(className = "p-2 rounded overflow-auto") {
                                setStyle("background-color", "#2d3142")
                                setStyle("min-height", "64.px")
                                finalAnswerText = div {
                                    content = "Запустите алгоритм, чтобы увидеть ответ."
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
        // Создаём скрытый input программно, чтобы не держать отдельный элемент на странице.
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
            // Ожидаемый формат: { "vertices": [...], "edges": [...] }.
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

        // Поддерживаются два варианта вершины: строка "A" или объект { id, x, y }.
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

            // Ребро можно записать массивом ["A", "B"] или объектом { from, to }.
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
        // Проверяем ссылки рёбер до очистки текущего графа, чтобы не потерять старый граф при ошибке.
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
        autoRunTimerId?.let { window.clearInterval(it) }
        autoRunTimerId = null
        lastAlgorithmResult = null
        isResetAvailable = false
        isDeleteVertexMode = false
        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        stopButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        saveResultButton.disabled = true
        deleteVertexButton.disabled = false
        finalAnswerText.content = "Запустите алгоритм, чтобы увидеть ответ."
        editStatusText.content = "ЛКМ по пустому месту - добавить вершину. ПКМ по двум вершинам - добавить ребро."
        resetEdgeSelection()

        // После успешной проверки полностью заменяем граф на импортированный.
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
                "color" to idleNodeColor(),
                "borderWidth" to 2
            ))
        }

        edges.forEachIndexed { index, edge ->
            currentEdges.add(json(
                "id" to "e_${edge.from}_${edge.to}_$index",
                "from" to edge.from,
                "to" to edge.to,
                "label" to "${edge.from} -> ${edge.to}",
                "color" to json("color" to colorEdge)
            ))
        }

        nodeCounter = calculateNextNodeCounter(vertices.map { it.id })
        statusText.content = "Граф загружен из файла: ${vertices.size} вершин, ${edges.size} рёбер."
        fitGraphToCanvas()
    }

    private fun buildCenteredPositions(vertices: List<ImportedVertex>): List<GraphPosition> {
        // Если координат в JSON нет, раскладываем вершины сеткой и центрируем граф на холсте.
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
        // Небольшая задержка нужна, чтобы vis-network успел получить новые вершины и рёбра.
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
        // Ограничение на длину защищает интерфейс от слишком длинных подписей вершин.
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
        // Координаты в JSON не обязательны, без них вершины будут расставлены автоматически.
        if (value == null || jsTypeOf(value) == "undefined") {
            return null
        }
        val number = js("Number(value)").unsafeCast<Double>()
        return if (js("Number.isFinite(number)").unsafeCast<Boolean>()) number.toInt() else null
    }

    private fun isArray(value: dynamic): Boolean = js("Array.isArray(value)").unsafeCast<Boolean>()

    private fun calculateNextNodeCounter(ids: List<String>): Int {
        // Новые вершины получают следующий свободный числовой id.
        val maxNumericId = ids.mapNotNull { it.toIntOrNull() }.maxOrNull()
        return (maxNumericId ?: 0) + 1
    }

    private fun startTopologicalSort() {
        pauseAutoRun()
        isResetAvailable = false
        // Берём актуальное состояние прямо из vis-network, чтобы учитывать ручные правки на холсте.
        val rawNodes = try {
            currentNodes.get()
        } catch (e: Exception) {
            statusText.content = "Ошибка выгрузки вершин: ${e.message}"
            return
        }

        val rawEdges = try {
            currentEdges.get()
        } catch (e: Exception) {
            statusText.content = "Ошибка выгрузки рёбер: ${e.message}"
            return
        }

        if (rawNodes.isEmpty()) {
            statusText.content = "Ошибка: невозможно запустить алгоритм на пустом графе."
            return
        }

        val payload = buildBackendRequest(rawNodes, rawEdges)
        startSortButton.disabled = true
        startSortButton.text = "Запрос к backend..."
        deleteVertexButton.disabled = true
        stopButton.disabled = true
        statusText.content = "Граф отправлен на backend. Ожидаем результат алгоритма..."

        window.asDynamic().fetch(
            "/api/topsort",
            json(
                "method" to "POST",
                "headers" to json("Content-Type" to "application/json"),
                "body" to JSON.stringify(payload)
            )
        ).then({ response: dynamic ->
            if (response.ok.unsafeCast<Boolean>()) {
                response.json().then({ data: dynamic ->
                    applyBackendResult(parseBackendResult(data))
                    null
                })
            } else {
                response.text().then({ text: dynamic ->
                    restoreAfterBackendError("Ошибка backend: ${text.toString()}")
                    null
                })
            }
            null
        }).catch({ error: dynamic ->
            restoreAfterBackendError("Не удалось подключиться к backend: ${error.toString()}")
            null
        })
    }

    private fun buildBackendRequest(rawNodes: Array<Json>, rawEdges: Array<Json>): Json {
        val nodes = rawNodes.map { node ->
            json(
                "id" to node["id"].toString(),
                "x" to readJsonInt(node["x"]),
                "y" to readJsonInt(node["y"])
            )
        }.toTypedArray()

        val edges = rawEdges.mapIndexed { index, edge ->
            json(
                "id" to (edge["id"]?.toString() ?: "edge_$index"),
                "from" to edge["from"].toString(),
                "to" to edge["to"].toString()
            )
        }.toTypedArray()

        return json("nodes" to nodes, "edges" to edges)
    }

    private fun parseBackendResult(data: dynamic): AlgorithmResult {
        val steps = mutableListOf<VisualStep>()
        val rawSteps = data.steps
        for (index in 0 until rawSteps.length.unsafeCast<Int>()) {
            val step = rawSteps[index]
            steps += VisualStep(
                description = step.description.toString(),
                nodes = parseBackendNodes(step.nodes),
                edges = parseBackendEdges(step.edges)
            )
        }

        return AlgorithmResult(
            hasCycle = data.hasCycle.unsafeCast<Boolean>(),
            finalOrder = parseStringList(data.finalOrder),
            steps = steps
        )
    }

    private fun parseBackendNodes(rawNodes: dynamic): List<VisualNode> {
        val nodes = mutableListOf<VisualNode>()
        for (index in 0 until rawNodes.length.unsafeCast<Int>()) {
            val node = rawNodes[index]
            nodes += VisualNode(
                id = node.id.toString(),
                label = node.label.toString(),
                x = readDynamicInt(node.x),
                y = readDynamicInt(node.y),
                color = node.color.toString()
            )
        }
        return nodes
    }

    private fun parseBackendEdges(rawEdges: dynamic): List<VisualEdge> {
        val edges = mutableListOf<VisualEdge>()
        for (index in 0 until rawEdges.length.unsafeCast<Int>()) {
            val edge = rawEdges[index]
            edges += VisualEdge(
                id = edge.id.toString(),
                from = edge.from.toString(),
                to = edge.to.toString(),
                color = edge.color.toString(),
                label = edge.label.toString(),
                isRemoved = edge.isRemoved.unsafeCast<Boolean>(),
                width = readDynamicInt(edge.width)
            )
        }
        return edges
    }

    private fun parseStringList(rawValues: dynamic): List<String> {
        val values = mutableListOf<String>()
        for (index in 0 until rawValues.length.unsafeCast<Int>()) {
            values += rawValues[index].toString()
        }
        return values
    }

    private fun readDynamicInt(value: dynamic): Int {
        val number = js("Number(value)").unsafeCast<Double>()
        return if (js("Number.isFinite(number)").unsafeCast<Boolean>()) number.toInt() else 0
    }

    private fun applyBackendResult(result: AlgorithmResult) {
        lastAlgorithmResult = result
        stepsList.clear()
        stepsList.addAll(result.steps)
        currentStepIndex = 0
        isExecutionMode = true
        isDeleteVertexMode = false

        setFinalAnswer(if (result.hasCycle) {
            "Топологическая сортировка невозможна: в графе есть цикл."
        } else {
            result.finalOrder.joinToString(" -> ")
        })

        startSortButton.disabled = true
        startSortButton.text = "Выполняется..."
        saveResultButton.disabled = false
        deleteVertexButton.disabled = true
        stopButton.disabled = false
        updateUiForCurrentStep()
        startAutoRun()
    }

    private fun restoreAfterBackendError(message: String) {
        pauseAutoRun()
        isExecutionMode = false
        isDeleteVertexMode = false
        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        deleteVertexButton.disabled = false
        saveResultButton.disabled = lastAlgorithmResult == null
        statusText.content = message
    }

    private fun startAutoRun() {
        pauseAutoRun()
        // Таймер просто двигает индекс шага; вся логика алгоритма уже посчитана заранее.
        autoRunTimerId = window.setInterval({
            if (currentStepIndex >= stepsList.lastIndex) {
                pauseAutoRun(allowReset = true)
            } else {
                currentStepIndex++
                updateUiForCurrentStep()
            }
        }, 900)
        stopButton.disabled = false
    }

    private fun pauseAutoRun(message: String? = null, allowReset: Boolean = false) {
        autoRunTimerId?.let { window.clearInterval(it) }
        autoRunTimerId = null
        if (::stopButton.isInitialized) {
            stopButton.disabled = true
        }
        // После остановки или завершения главная кнопка становится кнопкой полного сброса.
        if (allowReset && ::startSortButton.isInitialized) {
            isResetAvailable = true
            startSortButton.disabled = false
            startSortButton.text = "Сбросить граф"
        }
        if (message != null) {
            statusText.content = message
        }
    }

    private fun resetApplicationState() {
        pauseAutoRun()
        // Возвращаем приложение в состояние сразу после открытия страницы.
        isExecutionMode = false
        isDeleteVertexMode = false
        isResetAvailable = false
        selectedSourceNodeId = null
        stepsList.clear()
        currentStepIndex = 0
        lastAlgorithmResult = null
        nodeCounter = 1

        currentNodes.clear()
        currentEdges.clear()

        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        saveResultButton.disabled = true
        deleteVertexButton.disabled = false
        deleteVertexButton.text = "Удалить вершину"
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        stopButton.disabled = true

        statusText.content = "Ожидание построения графа и запуска..."
        finalAnswerText.content = "Запустите алгоритм, чтобы увидеть ответ."
        editStatusText.content = "ЛКМ по пустому месту - добавить вершину. ПКМ по двум вершинам - добавить ребро."
    }

    private fun saveResultToFile() {
        val result = lastAlgorithmResult
        if (result == null) {
            statusText.content = "Сначала запустите алгоритм."
            return
        }

        val text = buildString {
            appendLine("Топологическая сортировка алгоритмом Кана")
            appendLine()
            if (result.hasCycle) {
                appendLine("Итог: сортировка невозможна, в графе есть цикл.")
            } else {
                appendLine("Итог: ${result.finalOrder.joinToString(" -> ")}")
            }
            appendLine()
            appendLine("Шаги:")
            result.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.description}")
            }
        }

        // Blob позволяет скачать текстовый файл без сервера.
        val blob = js("new Blob([text], { type: 'text/plain;charset=utf-8' })")
        val url = js("window.URL.createObjectURL(blob)")
        val link = document.createElement("a") as HTMLElement
        link.asDynamic().href = url
        link.asDynamic().download = "topsort-result.txt"
        link.click()
        window.setTimeout({ js("window.URL.revokeObjectURL(url)") }, 0)
        statusText.content = "Результат сохранён в файл topsort-result.txt."
    }

    private fun showAboutDevelopers() {
        val memeUrl = "meme.jpg"
        // Старое окно удаляем, чтобы повторное нажатие не создавало несколько одинаковых модалок.
        document.getElementById("aboutModal")?.let { it.parentNode?.removeChild(it) }
        val modal = document.createElement("div") as HTMLElement
        modal.id = "aboutModal"
        modal.innerHTML = """
            <div style="position:fixed;inset:0;background:rgba(35,39,51,.82);z-index:9999;display:flex;align-items:center;justify-content:center;">
              <div style="background:#393d4f;color:#a594f9;border-radius:14px;padding:22px;max-width:460px;width:90%;box-shadow:0 18px 50px #0008;font-family:monospace;">
                <button onclick="document.getElementById('aboutModal').remove()" style="float:right;background:#2d3142;color:#a594f9;border:1px solid #4f546b;border-radius:8px;">×</button>
                <h3 style="margin-top:0;">О разработчиках</h3>
                <p>1. Купчинский Ян Станиславович, группа 4342</p>
                <p>2. Садыков Семён Русланович, группа 4342</p>
                <p>3. Тиссен Михаил Юрьевич, группа 4342</p>
                <img src="$memeUrl" onerror="this.style.display='none'" style="width:100%;max-height:260px;object-fit:contain;border-radius:10px;margin-top:10px;">
              </div>
            </div>
        """.trimIndent()
        document.body?.appendChild(modal)
    }

    private fun toggleDeleteVertexMode() {
        if (isExecutionMode) {
            editStatusText.content = "Во время выполнения алгоритма граф редактировать нельзя."
            return
        }
        resetEdgeSelection()
        isDeleteVertexMode = !isDeleteVertexMode
        deleteVertexButton.text = if (isDeleteVertexMode) "Отмена удаления" else "Удалить вершину"
        editStatusText.content = if (isDeleteVertexMode) {
            "Режим удаления: нажмите ЛКМ по вершине, которую нужно удалить."
        } else {
            "Удаление отменено. ЛКМ по пустому месту - добавить вершину."
        }
    }

    private fun deleteVertex(nodeId: String) {
        // Сначала удаляем все связанные рёбра, иначе в DataSet останутся ссылки на несуществующую вершину.
        val incidentEdges = currentEdges.get()
            .filter { edge -> edge["from"].toString() == nodeId || edge["to"].toString() == nodeId }
            .mapNotNull { edge -> edge["id"]?.toString() }

        incidentEdges.forEach { edgeId ->
            currentEdges.remove(edgeId)
        }
        currentNodes.remove(nodeId)

        selectedSourceNodeId = null
        isDeleteVertexMode = false
        isExecutionMode = false
        isResetAvailable = false
        deleteVertexButton.text = "Удалить вершину"
        lastAlgorithmResult = null
        stepsList.clear()
        currentStepIndex = 0
        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        saveResultButton.disabled = true
        finalAnswerText.content = "Граф изменён. Запустите алгоритм заново."
        statusText.content = "Удалена вершина $nodeId и ${incidentEdges.size} связанных рёбер."
        editStatusText.content = "Вершина $nodeId удалена."
    }

    private fun readJsonInt(value: Any?): Int {
        val number = value?.unsafeCast<Double>()
        return number?.toInt() ?: 0
    }

    private fun setFinalAnswer(text: String) {
        finalAnswerText.getElement()?.textContent = text
    }

    private fun navigateStep(direction: Int) {
        // Ручная навигация останавливает автопоказ, но оставляет возможность сбросить граф.
        pauseAutoRun(allowReset = true)
        val newIndex = currentStepIndex + direction
        if (newIndex in 0 until stepsList.size) {
            currentStepIndex = newIndex
            updateUiForCurrentStep()
        }
    }

    // Перерисовывает холст по заранее сохранённому состоянию текущего шага
    private fun updateUiForCurrentStep() {
        if (stepsList.isEmpty()) return
        val currentStep = stepsList[currentStepIndex]

        statusText.content = currentStep.description

        // Проще пересобрать DataSet заново, чем точечно искать все изменения между шагами.
        currentNodes.clear()
        currentEdges.clear()

        currentStep.nodes.forEach { node ->
            currentNodes.add(json(
                "id" to node.id,
                "label" to node.label,
                "x" to node.x,
                "y" to node.y,
                "color" to node.color,
                "borderWidth" to 2
            ))
        }

        currentStep.edges.forEach { edge ->
            currentEdges.add(json(
                "id" to edge.id,
                "from" to edge.from,
                "to" to edge.to,
                "label" to edge.label,
                "color" to json("color" to edge.color),
                "dashes" to edge.isRemoved,
                "width" to edge.width
            ))
        }

        stepBackwardButton.disabled = currentStepIndex == 0
        stepForwardButton.disabled = currentStepIndex == stepsList.size - 1
        stopButton.disabled = autoRunTimerId == null
    }

    private fun exitExecutionMode() {
        // Возвращаем обычный интерактивный холст после остановки симуляции.
        resetExecutionControls()
        statusText.content = "Режим симуляции прерван. Вы можете редактировать граф."
        initInteractiveGraph()
    }

    private fun resetExecutionControls() {
        pauseAutoRun()
        // Оставляем последний результат для сохранения, но снимаем режим показа шагов.
        isExecutionMode = false
        isResetAvailable = false
        stepsList.clear()
        currentStepIndex = 0
        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        saveResultButton.disabled = lastAlgorithmResult == null
        stopButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        deleteVertexButton.disabled = false
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun initInteractiveGraph() {
        val container = document.getElementById("networkView") as? HTMLElement ?: return
        networkInstance?.destroy()

        // При пересоздании холста начинаем с чистых DataSet, чтобы не смешивать старый граф с новым.
        currentNodes = VisDataSet(emptyArray())
        currentEdges = VisDataSet(emptyArray())
        nodeCounter = 1

        val data = json("nodes" to currentNodes, "edges" to currentEdges)
        // Физику выключаем, чтобы вершины оставались там, куда их поставил пользователь.
        val options = json(
            "physics" to json("enabled" to false),
            "nodes" to json(
                "borderWidth" to 2,
                "font" to json("color" to "#232733", "bold" to true)
            ),
            "edges" to json(
                "arrows" to "to",
                "color" to json("color" to colorEdge),
                "font" to json("color" to colorEdge, "strokeColor" to "#2d3142")
            ),
            "interaction" to json("dragNodes" to true, "dragView" to true, "zoomView" to true),
            "manipulation" to json("enabled" to false)
        )

        val network = VisNetwork(container, data, options)
        networkInstance = network

        // Браузерное меню по ПКМ мешает созданию рёбер, поэтому отключаем его внутри холста.
        container.oncontextmenu = { event ->
            event.preventDefault()
            false
        }

        network.on("click") { paramsJson ->
            if (isExecutionMode) return@on

            // В Kotlin/JS параметры события vis-network удобнее читать как dynamic.
            val params = paramsJson.unsafeCast<dynamic>()

            val pointerDom = params.pointer.DOM
            val clickedNodeId = network.getNodeAt(pointerDom.unsafeCast<Json>())

            if (isDeleteVertexMode) {
                if (clickedNodeId == null) {
                    editStatusText.content = "Нажмите именно на вершину, чтобы удалить её."
                } else {
                    deleteVertex(clickedNodeId)
                }
                return@on
            }

            if (clickedNodeId == null) {
                resetEdgeSelection()
                val canvasX = params.pointer.canvas.x
                val canvasY = params.pointer.canvas.y
                val newNodeId = "$nodeCounter"

                // Клик по пустому месту добавляет новую вершину именно в эту точку холста.
                currentNodes.add(json(
                    "id" to newNodeId,
                    "label" to newNodeId,
                    "x" to canvasX,
                    "y" to canvasY,
                    "color" to idleNodeColor(),
                    "borderWidth" to 2
                ))
                nodeCounter++
                markGraphEdited()
                editStatusText.content = "Добавлена вершина $newNodeId."
            } else {
                // ЛКМ по вершине только показывает подсказку, рёбра создаются через ПКМ.
                resetEdgeSelection()
                editStatusText.content = "Вершина $clickedNodeId выбрана. ПКМ по вершинам создаёт ребро."
            }
        }

        network.on("oncontext") { paramsJson ->
            if (isExecutionMode) return@on

            // ПКМ по первой вершине выбирает начало ребра, ПКМ по второй создаёт ребро.
            val params = paramsJson.unsafeCast<dynamic>()

            val pointerDom = params.pointer.DOM
            val clickedNodeId = network.getNodeAt(pointerDom.unsafeCast<Json>())

            if (clickedNodeId != null) {
                val currentSource = selectedSourceNodeId
                if (currentSource == null) {
                    selectedSourceNodeId = clickedNodeId
                    highlightEdgeSource(clickedNodeId)
                    editStatusText.content = "Источник ребра: $clickedNodeId. Нажмите ПКМ по целевой вершине."
                } else {
                    val target = clickedNodeId
                    if (currentSource != target) {
                        highlightEdgeTarget(target)
                        val edgeId = "e_${currentSource}_$target"
                        // id ребра собирается из двух вершин, чтобы vis-network мог потом обновлять или удалять его.
                        currentEdges.add(json(
                            "id" to edgeId,
                            "from" to currentSource,
                            "to" to target,
                            "label" to "$currentSource -> $target",
                            "color" to json("color" to colorEdge)
                        ))
                        markGraphEdited()
                        editStatusText.content = "Добавлено ребро $currentSource -> $target."
                    }
                    resetEdgeSelection()
                }
            } else {
                resetEdgeSelection()
            }
        }
    }

    private fun resetEdgeSelection() {
        // Если источник ребра был выбран, возвращаем ему обычную рамку.
        val currentSource = selectedSourceNodeId
        if (currentSource != null) {
            restoreBuildNodeStyle(currentSource)
            selectedSourceNodeId = null
        }
    }

    private fun idleNodeColor(): Json = json(
        // Обычный цвет вершины в режиме редактирования графа.
        "background" to colorIdle,
        "border" to "#4f546b",
        "highlight" to json(
            "background" to colorIdle,
            "border" to colorSelectionBorder
        )
    )

    private fun selectedNodeColor(): Json = json(
        // Выбранная вершина подсвечивается рамкой, чтобы было видно начало будущего ребра.
        "background" to colorIdle,
        "border" to colorSelectionBorder,
        "highlight" to json(
            "background" to colorIdle,
            "border" to colorSelectionBorder
        )
    )

    private fun highlightEdgeSource(nodeId: String) {
        currentNodes.update(json(
            "id" to nodeId,
            "color" to selectedNodeColor(),
            "borderWidth" to 5
        ))
    }

    private fun highlightEdgeTarget(nodeId: String) {
        currentNodes.update(json(
            "id" to nodeId,
            "color" to selectedNodeColor(),
            "borderWidth" to 5
        ))
        // Короткая подсветка показывает, куда было добавлено ребро.
        window.setTimeout({
            if (!isExecutionMode && selectedSourceNodeId == null) {
                restoreBuildNodeStyle(nodeId)
            }
        }, 450)
    }

    private fun restoreBuildNodeStyle(nodeId: String) {
        if (!isExecutionMode) {
            currentNodes.update(json(
                "id" to nodeId,
                "color" to idleNodeColor(),
                "borderWidth" to 2
            ))
        }
    }

    private fun markGraphEdited() {
        // Любая правка графа делает старый результат недействительным.
        isExecutionMode = false
        isResetAvailable = false
        lastAlgorithmResult = null
        stepsList.clear()
        currentStepIndex = 0
        startSortButton.disabled = false
        startSortButton.text = "Топологическая сортировка >>>"
        saveResultButton.disabled = true
        stepBackwardButton.disabled = true
        stepForwardButton.disabled = true
        finalAnswerText.content = "Граф изменён. Запустите алгоритм заново."
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
