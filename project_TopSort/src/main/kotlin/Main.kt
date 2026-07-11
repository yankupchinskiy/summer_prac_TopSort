import graphCL.Graph
import graphCL.GraphJsonParser
import graphCL.GraphParseException
import logic.SortMode
import logic.topologicalSortKahn
import java.io.File

// Читает граф из JSON-файла и преобразует его во внутреннюю модель Graph.
// Возвращает null, если файл не удалось прочитать
// или JSON имеет неверный формат.
fun parceJson(path: String): Graph? {
    val input = try {
        File(path).readText(Charsets.UTF_8)
    } catch (e: Exception) {
        System.err.println("Failed to read file '$path': ${e.message}")
        return null
    }

    val graph = try {
        GraphJsonParser.parse(input)
    } catch (e: GraphParseException) {
        System.err.println("Parse error: ${e.message}")
        return null
    }

    // Выводим загруженный граф, чтобы сразу проверить входные данные.
    println("Graph loaded successfully.")
    println("Vertices: ${graph.vertices.size}")
    for (vertex in graph.vertices.values) {
        println("  ${vertex.id.value}: x=${vertex.x}, y=${vertex.y}")
    }
    println("Edges: ${graph.edges.size}")
    for (edge in graph.edges) {
        println("  ${edge.from.value} -> ${edge.to.value}")
    }

    return graph
}

fun main(args: Array<String>) {
    // Выбираем режим показа алгоритма:
    // полностью автоматический или пошаговый.
    val sortFlag = SortMode.AUTOMATIC

    // Если путь не передан явно, берём graph.json
    // из текущей рабочей папки.
    val path = args.firstOrNull() ?: "graph.json"

    if (args.isEmpty()) {
        println("No file path provided, using '$path'.")
    }

    val graph = parceJson(path)

    if (graph == null) {
        println("Не удалось загрузить граф. Завершение работы.")
        return
    }

    // После загрузки графа запускаем выбранный режим сортировки.
    if (sortFlag == SortMode.AUTOMATIC) {
        println(" АВТОМАТИЧЕСКИЙ РЕЖИМ ")
        val autoResult = graph.topologicalSortKahn(mode = SortMode.AUTOMATIC)
        if (autoResult != null) {
            println("\nИтог автоматического режима: ${autoResult.map { it.value }}")
        } else {
            println("\nАвтоматический режим: Топологическая сортировка невозможна (в графе есть цикл).")
        }
    } else if (sortFlag == SortMode.STEP_BY_STEP) {
        println("\n\n ПОШАГОВЫЙ РЕЖИМ ")
        val stepByStepResult = graph.topologicalSortKahn(mode = SortMode.STEP_BY_STEP)
        if (stepByStepResult != null) {
            println("\nИтог пошагового режима: ${stepByStepResult.map { it.value }}")
        } else {
            println("\nПошаговый режим: Топологическая сортировка невозможна (в графе есть цикл).")
        }
    }
}
