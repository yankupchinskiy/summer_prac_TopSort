import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val colorIdle = "#8b90a3"
private const val colorActive = "#7bd88f"
private const val colorProcessed = "#5bc0de"
private const val colorCycle = "#f07178"
private const val colorEdge = "#a594f9"
private const val colorRemovedEdge = "#4f546b"

@Serializable
data class GraphRequest(
    val nodes: List<RequestNode>,
    val edges: List<RequestEdge>
)

@Serializable
data class RequestNode(
    val id: String,
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
data class RequestEdge(
    val id: String? = null,
    val from: String,
    val to: String
)

@Serializable
data class AlgorithmResponse(
    val hasCycle: Boolean,
    val finalOrder: List<String>,
    val steps: List<VisualStep>
)

@Serializable
data class VisualNode(
    val id: String,
    val label: String,
    val x: Int,
    val y: Int,
    val color: String
)

@Serializable
data class VisualEdge(
    val id: String,
    val from: String,
    val to: String,
    val color: String,
    val label: String,
    val isRemoved: Boolean,
    val width: Int
)

@Serializable
data class VisualStep(
    val description: String,
    val nodes: List<VisualNode>,
    val edges: List<VisualEdge>
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
        }
        routing {
            get("/") {
                call.respond(mapOf("status" to "Backend для топологической сортировки работает"))
            }
            post("/api/topsort") {
                val request = call.receive<GraphRequest>()
                val validationError = validateGraph(request)
                if (validationError != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to validationError))
                    return@post
                }

                call.respond(buildAlgorithmResult(request))
            }
        }
    }.start(wait = true)
}

private fun validateGraph(request: GraphRequest): String? {
    if (request.nodes.isEmpty()) {
        return "Граф должен содержать хотя бы одну вершину."
    }

    val ids = mutableSetOf<String>()
    request.nodes.forEach { node ->
        if (node.id.isBlank()) {
            return "В графе есть вершина с пустым id."
        }
        if (!ids.add(node.id)) {
            return "В графе есть повторяющаяся вершина '${node.id}'."
        }
    }

    request.edges.forEach { edge ->
        if (!ids.contains(edge.from)) {
            return "Ребро ссылается на неизвестную вершину '${edge.from}'."
        }
        if (!ids.contains(edge.to)) {
            return "Ребро ссылается на неизвестную вершину '${edge.to}'."
        }
    }

    return null
}

private fun buildAlgorithmResult(request: GraphRequest): AlgorithmResponse {
    val nodeIds = request.nodes.map { it.id }
    val edges = request.edges.mapIndexed { index, edge ->
        edge to (edge.id ?: "edge_$index")
    }

    // inDegree показывает, сколько входящих рёбер осталось у каждой вершины.
    val inDegrees = nodeIds.associateWith { 0 }.toMutableMap()
    edges.forEach { (edge, _) ->
        inDegrees[edge.to] = (inDegrees[edge.to] ?: 0) + 1
    }

    val adjacency = nodeIds.associateWith { mutableListOf<Int>() }
    edges.forEachIndexed { index, pair ->
        adjacency[pair.first.from]?.add(index)
    }

    // В очереди лежат вершины, которые уже можно добавить в ответ.
    val queue = ArrayDeque<String>()
    nodeIds.filter { inDegrees[it] == 0 }.forEach { queue.addLast(it) }
    val processed = mutableSetOf<String>()
    val removedEdges = mutableSetOf<Int>()
    val order = mutableListOf<String>()
    val steps = mutableListOf<VisualStep>()

    steps += createVisualStep(
        request = request,
        edges = edges,
        inDegrees = inDegrees,
        processed = processed,
        available = queue.toSet(),
        activeNode = null,
        activeEdges = emptySet(),
        removedEdges = removedEdges,
        cycleNodes = emptySet(),
        description = "Шаг 0: рассчитаны входящие степени. Доступные вершины: ${formatList(queue.toList())}."
    )

    // Каждый проход имитирует удаление одной вершины и всех исходящих из неё рёбер.
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val outgoingIndexes = adjacency[current].orEmpty()

        order += current
        processed += current

        outgoingIndexes.forEach { edgeIndex ->
            val target = edges[edgeIndex].first.to
            inDegrees[target] = (inDegrees[target] ?: 0) - 1
            removedEdges += edgeIndex
            if (inDegrees[target] == 0) {
                queue.addLast(target)
            }
        }

        steps += createVisualStep(
            request = request,
            edges = edges,
            inDegrees = inDegrees,
            processed = processed,
            available = queue.toSet(),
            activeNode = current,
            activeEdges = outgoingIndexes.toSet(),
            removedEdges = removedEdges,
            cycleNodes = emptySet(),
            description = "Обрабатываем вершину $current. Текущий порядок: ${formatList(order)}. Удалённые рёбра: ${formatEdges(edges, outgoingIndexes)}."
        )
    }

    val cycleNodes = nodeIds.filter { !processed.contains(it) }.toSet()
    val hasCycle = processed.size != nodeIds.size
    if (hasCycle) {
        steps += createVisualStep(
            request = request,
            edges = edges,
            inDegrees = inDegrees,
            processed = processed,
            available = emptySet(),
            activeNode = null,
            activeEdges = emptySet(),
            removedEdges = removedEdges,
            cycleNodes = cycleNodes,
            description = "Алгоритм остановился: остались вершины с входящими рёбрами. Цикл/зависимость от цикла: ${formatList(cycleNodes.toList())}."
        )
    }

    return AlgorithmResponse(hasCycle = hasCycle, finalOrder = order, steps = steps)
}

private fun createVisualStep(
    request: GraphRequest,
    edges: List<Pair<RequestEdge, String>>,
    inDegrees: Map<String, Int>,
    processed: Set<String>,
    available: Set<String>,
    activeNode: String?,
    activeEdges: Set<Int>,
    removedEdges: Set<Int>,
    cycleNodes: Set<String>,
    description: String
): VisualStep {
    val nodes = request.nodes.map { node ->
        val color = when {
            cycleNodes.contains(node.id) -> colorCycle
            node.id == activeNode -> colorActive
            processed.contains(node.id) -> colorProcessed
            available.contains(node.id) -> colorProcessed
            else -> colorIdle
        }

        VisualNode(
            id = node.id,
            label = "${node.id}\nin: ${inDegrees[node.id] ?: 0}",
            x = node.x,
            y = node.y,
            color = color
        )
    }

    val visualEdges = edges.mapIndexed { index, pair ->
        val edge = pair.first
        val color = when {
            activeEdges.contains(index) -> colorActive
            removedEdges.contains(index) -> colorRemovedEdge
            else -> colorEdge
        }

        VisualEdge(
            id = pair.second,
            from = edge.from,
            to = edge.to,
            color = color,
            label = "${edge.from} -> ${edge.to}",
            isRemoved = removedEdges.contains(index),
            width = if (activeEdges.contains(index)) 4 else if (removedEdges.contains(index)) 1 else 2
        )
    }

    return VisualStep(description, nodes, visualEdges)
}

private fun formatList(values: List<String>): String =
    if (values.isEmpty()) "нет" else values.joinToString(" -> ")

private fun formatEdges(edges: List<Pair<RequestEdge, String>>, indexes: List<Int>): String =
    if (indexes.isEmpty()) "нет" else indexes.joinToString(", ") { index ->
        val edge = edges[index].first
        "${edge.from} -> ${edge.to}"
    }
