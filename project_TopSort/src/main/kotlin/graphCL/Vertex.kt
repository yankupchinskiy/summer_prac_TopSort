package graphCL

// Отдельный тип для идентификатора вершины.
// Так код графа читается понятнее, чем при использовании String везде.
data class VertexId(val value: String)

// Вершина графа.
// x и y хранят координаты для будущей визуализации,
// inDegree пересчитывается во время топологической сортировки.
data class Vertex(
    val id: VertexId,
    val x: Int,
    val y: Int,
    var inDegree: Int = 0
)
