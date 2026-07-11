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
