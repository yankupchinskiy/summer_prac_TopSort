package graphCL

<<<<<<< HEAD
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Формат JSON, который ожидает программа:
// список идентификаторов вершин и список рёбер в виде пар [from, to].
@Serializable
private data class GraphPayload(
    val vertices: List<String>,
    val edges: List<List<String>>
)

// Преобразует текст JSON во внутреннюю модель Graph.
object GraphJsonParser {
    private val json = Json {
        ignoreUnknownKeys = false
    }

    fun parse(input: String): Graph {
        // Сначала проверяем, что JSON соответствует ожидаемой структуре.
        val payload = try {
            json.decodeFromString<GraphPayload>(input)
        } catch (e: SerializationException) {
            throw GraphParseException("Invalid JSON: ${e.message ?: "unknown error"}.")
        }

        if (payload.vertices.isEmpty()) {
            throw GraphParseException("JSON must contain at least one vertex.")
        }

        // Идентификаторы вершин должны быть непустыми и уникальными.
        val ids = payload.vertices.toMutableList()
        for ((index, id) in ids.withIndex()) {
            requireId(id, "Vertex at index $index")
        }

        if (ids.size != ids.toSet().size) {
            throw GraphParseException("JSON contains duplicate vertex ids.")
        }

        val vertices = buildVertices(ids)
        val graphEdges = mutableListOf<Edge>()

        // Каждое ребро должно содержать ровно два идентификатора,
        // и обе вершины уже должны существовать в списке вершин.
        for ((edgeIndex, pair) in payload.edges.withIndex()) {
            if (pair.size != 2) {
                throw GraphParseException("Edge at index $edgeIndex must contain exactly two vertex ids.")
            }

            val fromText = pair[0]
            val toText = pair[1]
            requireId(fromText, "Edge at index $edgeIndex source")
            requireId(toText, "Edge at index $edgeIndex target")

            val fromId = VertexId(fromText)
            val toId = VertexId(toText)

            if (!vertices.containsKey(fromId)) {
                throw GraphParseException("Edge at index $edgeIndex references unknown source vertex '$fromText'.")
            }
            if (!vertices.containsKey(toId)) {
                throw GraphParseException("Edge at index $edgeIndex references unknown target vertex '$toText'.")
            }

            graphEdges += Edge(from = fromId, to = toId)
        }

        return Graph(vertices = vertices, edges = graphEdges)
    }

    private fun requireId(id: String, owner: String) {
        if (id.isBlank()) {
            throw GraphParseException("$owner is empty.")
        }
    }

// Если координаты не заданы в JSON, расставляем вершины по сетке автоматически.
// Так граф сразу готов к выводу и будущей визуализации.
    private fun buildVertices(ids: List<String>): Map<VertexId, Vertex> {
        val columns = maxOf(1, kotlin.math.ceil(kotlin.math.sqrt(ids.size.toDouble())).toInt())
        val horizontalGap = 180
        val verticalGap = 120
        val startX = 100
        val startY = 100

        val result = linkedMapOf<VertexId, Vertex>()
        for (index in ids.indices) {
            val idText = ids[index]
            val row = index / columns
            val column = index % columns
            val id = VertexId(idText)

            result[id] = Vertex(
                id = id,
                x = startX + column * horizontalGap,
                y = startY + row * verticalGap
            )
        }
        return result
    }
}
=======
class GraphParseException(message: String) : Exception(message)
>>>>>>> 6dd10241cc267093e595fcc4cc196ca66d446d91
