package logic

import kotlinx.serialization.Serializable

@Serializable
data class VertexPayload(val id: String, val x: Int, val y: Int)

@Serializable
data class EdgePayload(val from: String, val to: String)

@Serializable
data class GraphRequest(val vertices: List<VertexPayload>, val edges: List<EdgePayload>)

// Описание одного шага для фронтенда
@Serializable
data class AlgorithmStepDto(
    val description: String,
    val nodes: List<JsonNodeState>,
    val edges: List<JsonEdgeState>
)

@Serializable
data class JsonNodeState(val id: String, val label: String, val x: Int, val y: Int, val color: String)

@Serializable
data class JsonEdgeState(val id: String, val from: String, val to: String, val color: String)

@Serializable
data class GraphResponse(
    val hasCycle: Boolean,
    val steps: List<AlgorithmStepDto>
)