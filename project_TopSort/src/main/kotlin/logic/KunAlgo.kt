package logic

<<<<<<< HEAD
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
=======
import graphCL.*


// Режимы выполнения 
enum class SortMode {
    STEP_BY_STEP, 
    AUTOMATIC     
}



>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
fun Graph.calculateInDegrees() {
    vertices.values.forEach { it.inDegree = 0 }

    edges.forEach { edge ->
        vertices[edge.to]?.let { it.inDegree++ }
    }
}

<<<<<<< HEAD
// Строит список смежности, чтобы быстро находить все исходящие соседние вершины
// для текущей вершины во время работы алгоритма.
=======

>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
fun Graph.buildAdjacencyList(): Map<VertexId, List<VertexId>> {
    val adjList = vertices.keys.associateWith { mutableListOf<VertexId>() }
    edges.forEach { edge ->
        adjList[edge.from]?.add(edge.to)
    }
    return adjList
}

<<<<<<< HEAD
// Собирает все вершины без входящих рёбер.
// Именно они могут идти следующими в топологическом порядке.
=======

>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
fun Graph.initializeStack(): ArrayDeque<VertexId> {
    val stack = ArrayDeque<VertexId>()
    vertices.values
        .filter { it.inDegree == 0 }
<<<<<<< HEAD
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
=======
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
>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
    }
    println("-------------------------------")
}

<<<<<<< HEAD
// Ждёт ввод пользователя в пошаговом режиме,
// чтобы следующее действие выполнялось только после подтверждения.
=======

>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
fun waitForUser(message: String) {
    println(message)
    readln()
}

<<<<<<< HEAD
// Топологическая сортировка Кана.
// Возвращает топологический порядок, если граф ацикличен.
// Возвращает null, если из-за цикла удалось обработать не все вершины.
fun Graph.topologicalSortKahn(mode: SortMode): List<VertexId>? {
=======

fun Graph.topologicalSortKahn(mode: SortMode): List<VertexId>? {

>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
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

<<<<<<< HEAD
        // Берём вершину без входящих рёбер и добавляем её в ответ.
        val current = stack.removeLast()
        result.add(current)

        // Логически удаляем её исходящие рёбра:
        // уменьшаем inDegree у соседей и добавляем в стек любую вершину,
        // у которой больше не осталось входящих рёбер.
=======
        val current = stack.removeLast()
        result.add(current)

>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
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

<<<<<<< HEAD
    // Если обработаны не все вершины, значит в графе есть цикл.
=======
>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
    return if (result.size == vertices.size) result else null
}
