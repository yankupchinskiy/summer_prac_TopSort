package logic

import graphCL.Graph
import graphCL.Vertex
import graphCL.VertexId

// Режимы выполнения топологической сортировки.
// AUTOMATIC запускает алгоритм сразу.
// STEP_BY_STEP делает паузу перед каждым важным действием.
enum class SortMode {
    STEP_BY_STEP,
    AUTOMATIC
}

// Пересчитывает количество входящих рёбер для каждой вершины.
// Это основной подготовительный шаг алгоритма Кана.
fun Graph.calculateInDegrees() {
    vertices.values.forEach { it.inDegree = 0 }

    edges.forEach { edge ->
        vertices[edge.to]?.let { it.inDegree++ }
    }
}

// Строит список смежности, чтобы быстро находить все исходящие соседние вершины
// для текущей вершины во время работы алгоритма.
fun Graph.buildAdjacencyList(): Map<VertexId, List<VertexId>> {
    val adjList = vertices.keys.associateWith { mutableListOf<VertexId>() }
    edges.forEach { edge ->
        adjList[edge.from]?.add(edge.to)
    }
    return adjList
}

// Собирает все вершины без входящих рёбер.
// Именно они могут идти следующими в топологическом порядке.
fun Graph.initializeStack(): ArrayDeque<VertexId> {
    val stack = ArrayDeque<VertexId>()
    vertices.values
        .filter { it.inDegree == 0 }
        .forEach { stack.addLast(it.id) }
    return stack
}

// Выводит текущее состояние алгоритма для отладки и демонстрации:
// частичный результат, текущий стек и inDegree каждой вершины.
fun printState(
    step: Int,
    result: List<VertexId>,
    stack: ArrayDeque<VertexId>,
    vertices: Map<VertexId, Vertex>
) {
    println("\n Состояние на шаге $step ")
    println("Результат (отсортированные): ${result.map { it.value }}")
    println("Стек (верх справа ->): ${stack.toList().map { it.value }}")
    println("Текущие inDegree:")
    vertices.values.forEach { vertex ->
        println("  ${vertex.id.value}: ${vertex.inDegree}")
    }
    println("-------------------------------")
}

// Ждёт ввод пользователя в пошаговом режиме,
// чтобы следующее действие выполнялось только после подтверждения.
fun waitForUser(message: String) {
    println(message)
    readln()
}

// Топологическая сортировка Кана.
// Возвращает топологический порядок, если граф ацикличен.
// Возвращает null, если из-за цикла удалось обработать не все вершины.
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

        // Берём вершину без входящих рёбер и добавляем её в ответ.
        val current = stack.removeLast()
        result.add(current)

        // Логически удаляем её исходящие рёбра:
        // уменьшаем inDegree у соседей и добавляем в стек любую вершину,
        // у которой больше не осталось входящих рёбер.
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

    // Если обработаны не все вершины, значит в графе есть цикл.
    return if (result.size == vertices.size) result else null
}
