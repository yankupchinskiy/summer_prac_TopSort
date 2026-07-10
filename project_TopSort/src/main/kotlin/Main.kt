import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

data class VertexId(val value: String)

data class Vertex(
    val id: VertexId,
    val x: Int,
    val y: Int
)

data class Edge(
    val from: VertexId,
    val to: VertexId
)

data class Graph(
    val vertices: Map<VertexId, Vertex>,
    val edges: List<Edge>
)

class GraphParseException(message: String) : Exception(message)

@Serializable
private data class GraphPayload(
    val vertices: List<String>,
    val edges: List<List<String>>
)

object GraphJsonParser {
    private val json = Json {
        ignoreUnknownKeys = false
    }

    fun parse(input: String): Graph {
        val payload = try {
            json.decodeFromString<GraphPayload>(input)
        } catch (e: SerializationException) {
            throw GraphParseException("Invalid JSON: ${e.message ?: "unknown error"}.")
        }

        if (payload.vertices.isEmpty()) {
            throw GraphParseException("JSON must contain at least one vertex.")
        }

        val ids = payload.vertices.toMutableList()
        for ((index, id) in ids.withIndex()) {
            requireId(id, "Vertex at index $index")
        }

        if (ids.size != ids.toSet().size) {
            throw GraphParseException("JSON contains duplicate vertex ids.")
        }

        val vertices = buildVertices(ids)
        val graphEdges = mutableListOf<Edge>()

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

fun main(args: Array<String>) {
    val path = args.firstOrNull() ?: "graph.json"

    if (args.isEmpty()) {
        println("No file path provided, using '$path'.")
    }

    val input = try {
        File(path).readText(Charsets.UTF_8)
    } catch (e: Exception) {
        System.err.println("Failed to read file '$path': ${e.message}")
        return
    }

    val graph = try {
        GraphJsonParser.parse(input)
    } catch (e: GraphParseException) {
        System.err.println("Parse error: ${e.message}")
        return
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
}
