import graphCL.*
import logic.*
import java.io.File



fun parceJson(path: String): Graph? {
    val input = try {
        File(path).readText(Charsets.UTF_8)
    } catch (e: Exception) {
        System.err.println("Failed to read file '$path': ${e.message}")
        return null // Возвращаем null при ошибке чтения
    }

    val graph = try {
        GraphJsonParser.parse(input)
    } catch (e: GraphParseException) {
        System.err.println("Parse error: ${e.message}")
        return null // Возвращаем null при ошибке парсинга
    }

    println("Graph loaded successfully.")
    println("Vertices: ${graph.vertices.size}")
    for (vertex in graph.vertices.values) {
        println("  ${vertex.id.value}: x=${vertex.x}, y=${vertex.y}")
    }
    println("Edges: ${graph.edges.size}")
    for (edge in graph.edges) {
        println("  ${edge.from.value} -> ${edge.to.value}")
    }

    return graph // Возвращаем успешно распарсенный граф
}

fun main(args: Array<String>) {

    val SortFlag = SortMode.AUTOMATIC

    val path = args.firstOrNull() ?: "graph.json"

    if (args.isEmpty()) {
        println("No file path provided, using '$path'.")
    }

    val graph = parceJson(path)

    if (graph == null) {
        println("Не удалось загрузить граф. Завершение работы.")
        return
    }
    if (SortFlag == SortMode.AUTOMATIC) {
        println(" АВТОМАТИЧЕСКИЙ РЕЖИМ ")
        val autoResult = graph.topologicalSortKahn(mode = SortMode.AUTOMATIC)
        if (autoResult != null) {
            println("\nИтог автоматического режима: ${autoResult.map { it.value }}")
        } else {
            println("\nАвтоматический режим: Топологическая сортировка невозможна (в графе есть цикл).")
        }
    }
    else if (SortFlag == SortMode.STEP_BY_STEP) {
        println("\n\n ПОШАГОВЫЙ РЕЖИМ ")
        val stepByStepResult = graph.topologicalSortKahn(mode = SortMode.STEP_BY_STEP)
        if (stepByStepResult != null) {
            println("\nИтог пошагового режима: ${stepByStepResult?.map { it.value }}")
        } else {
            println("\nпошаговый режим: Топологическая сортировка невозможна (в графе есть цикл).")

        }
    }
}