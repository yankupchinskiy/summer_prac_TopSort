package classes

data class VertexId(val value: String)

data class Vertex(
    val id: VertexId,
    val x: Int,
    val y: Int
)