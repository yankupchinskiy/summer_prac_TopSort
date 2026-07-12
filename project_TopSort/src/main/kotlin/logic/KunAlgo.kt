package logic

import graphCL.*


// Режимы выполнения 
enum class SortMode {
    STEP_BY_STEP, 
    AUTOMATIC     
}



fun Graph.calculateInDegrees() {
    vertices.values.forEach { it.inDegree = 0 }

    edges.forEach { edge ->
        vertices[edge.to]?.let { it.inDegree++ }
    }
}


fun Graph.buildAdjacencyList(): Map<VertexId, List<VertexId>> {
    val adjList = vertices.keys.associateWith { mutableListOf<VertexId>() }
    edges.forEach { edge ->
        adjList[edge.from]?.add(edge.to)
    }
    return adjList
}


fun Graph.initializeStack(): ArrayDeque<VertexId> {
    val stack = ArrayDeque<VertexId>()
    vertices.values
        .filter { it.inDegree == 0 }
        .forEach { stack.addLast(it.id) } // Кладем в стек
    return stack
}


fun printState(step: Int, result: List<VertexId>, stack: ArrayDeque<VertexId>, vertices: Map<VertexId, Vertex>) {
    println("\n Состояние на шаге $step ")
    println("Результат (отсортированные): ${result.map { it.value }}")

    println("Стек (верх справа ->): ${stack.toList().map { it.value }}")

    println("Текущие inDegree:")
    vertices.values.forEach { v ->
        println("  ${v.id.value}: ${v.inDegree}")
    }
    println("-------------------------------")
}


fun waitForUser(message: String) {
    println(message)
    readln()
}


fun Graph.topologicalSortKahn(mode: SortMode): List<VertexId>? {

    calculateInDegrees()
    val adjList = buildAdjacencyList()
    val stack = initializeStack()
    val result = mutableListOf<VertexId>()

    printState(0, result, stack, vertices)

    if (mode == SortMode.STEP_BY_STEP) {
        waitForUser("Нажмите Enter для начала основного цикла алгоритма...")
    }

    var step = 1

    while (stack.isNotEmpty()) {
        if (mode == SortMode.STEP_BY_STEP) {
            waitForUser("Нажмите Enter для выполнения шага $step...")
        }

        val current = stack.removeLast()
        result.add(current)

        adjList[current]?.forEach { neighbor ->
            val neighborVertex = vertices[neighbor]
            if (neighborVertex != null) {
                neighborVertex.inDegree--
                if (neighborVertex.inDegree == 0) {
                    stack.addLast(neighbor)
                }
            }
        }

        printState(step, result, stack, vertices)
        step++
    }

    return if (result.size == vertices.size) result else null
}

fun Graph.generateVisualSteps(): GraphResponse {
    val steps = mutableListOf<AlgorithmStepDto>()

    // Считаем входящие степени и строим списки смежности как в оригинале
    calculateInDegrees()
    val adjList = buildAdjacencyList()
    val stack = initializeStack()
    val result = mutableListOf<VertexId>()

    val allNodeIds = vertices.keys.toList()
    val activeEdges = edges.toMutableList() // Ребра, которые еще не удалены физически

    // Вспомогательная функция сохранения слепка графа
    fun capture(desc: String, currentNode: VertexId? = null, newlyZeroNodes: List<VertexId> = emptyList()) {
        val nodesJson = allNodeIds.map { vId ->
            val v = vertices[vId]!!

            // Логика покраски вершин по спецификации
            val color = when {
                result.contains(vId) -> "#ababab"       // Вершина уже обработана (серая)
                vId == currentNode -> "#b94a48"         // Текущая вершина в обработке (красная)
                stack.contains(vId) -> "#dff0d8"        // Находится в стеке / Источник (зеленая)
                else -> "#5bc0de"                       // Обычная (голубая)
            }

            val label = "${vId.value} (in:${v.inDegree})"
            JsonNodeState(id = vId.value, label = label, x = v.x, y = v.y, color = color)
        }

        val edgesJson = edges.map { edge ->
            val edgeId = "e_${edge.from.value}_${edge.to.value}"
            // Если ребра уже нет в activeEdges, значит оно удалено алгоритмом
            val isDeleted = !activeEdges.contains(edge)
            val color = if (isDeleted) "#e0e0e0" else "#a594f9"
            JsonEdgeState(id = edgeId, from = edge.from.value, to = edge.to.value, color = color)
        }

        steps.add(AlgorithmStepDto(desc, nodesJson, edgesJson))
    }

    // Шаг 0: Инициализация
    capture("Начало: вычислены входящие степени. Истоки в стеке: ${stack.map { it.value }}")

    var stepCounter = 1
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        result.add(current)

        capture("Шаг $stepCounter: Извлекаем вершину ${current.value} из стека и добавляем в результат.", currentNode = current)

        val newlyZero = mutableListOf<VertexId>()

        adjList[current]?.forEach { neighbor ->
            val neighborVertex = vertices[neighbor]
            if (neighborVertex != null) {
                neighborVertex.inDegree--

                // Удаляем ребро из списка активных для визуализации
                activeEdges.remove(Edge(current, neighbor))

                if (neighborVertex.inDegree == 0) {
                    stack.addLast(neighbor)
                    newlyZero.add(neighbor)
                }
            }
        }

        if (newlyZero.isNotEmpty()) {
            capture("Шаг $stepCounter: Удалены ребра из ${current.value}. У вершин ${newlyZero.map { it.value }} степень стала 0, они добавлены в стек.", currentNode = current, newlyZeroNodes = newlyZero)
        } else {
            capture("Шаг $stepCounter: Удалены ребра из ${current.value}. Новых истоков не обнаружено.", currentNode = current)
        }

        stepCounter++
    }

    val hasCycle = result.size != vertices.size
    if (!hasCycle) {
        // Финальный шаг успеха
        val finalNodes = allNodeIds.map { vId ->
            val v = vertices[vId]!!
            JsonNodeState(id = vId.value, label = vId.value, x = v.x, y = v.y, color = "#dff0d8")
        }
        val finalEdges = edges.map { JsonEdgeState("e_${it.from.value}_${it.to.value}", it.from.value, it.to.value, "#e0e0e0") }
        steps.add(AlgorithmStepDto("Сортировка успешно завершена! Порядок: ${result.map { it.value }}", finalNodes, finalEdges))
    }

    return GraphResponse(hasCycle = hasCycle, steps = steps)
}