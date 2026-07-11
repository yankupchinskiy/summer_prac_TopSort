package graphCL

// Основная модель ориентированного графа,
// которую используют парсер и алгоритм сортировки.
// Вершины хранятся по id, а рёбра отдельно списком.
data class Graph(
    val vertices: Map<VertexId, Vertex>,
    val edges: List<Edge>
)
