package graphCL

data class Graph(
    val vertices: Map<VertexId, Vertex>,
    val edges: List<Edge>
)