import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*

// Явный импорт твоих модулей
import graphCL.Graph
import graphCL.Vertex
import graphCL.VertexId
import graphCL.Edge
import logic.GraphResponse
import logic.GraphRequest
import logic.generateVisualSteps

fun main() {
    println("Запуск сервера топологической сортировки на http://localhost:8080...")

    embeddedServer(Netty, port = 8080) {
        // Настройка CORS для общения с KVision фронтендом
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
        }

        // Поддержка JSON сериализации
        install(ContentNegotiation) {
            json()
        }

        routing {
            post("/api/topsort") {
                try {
                    val request = call.receive<GraphRequest>()

                    // Собираем граф из пришедшего с фронтенда JSON Payload
                    val verticesMap = request.vertices.associate { payload ->
                        val vId = VertexId(payload.id)
                        vId to Vertex(id = vId, x = payload.x, y = payload.y)
                    }

                    val edgesList = request.edges.map { payload ->
                        Edge(from = VertexId(payload.from), to = VertexId(payload.to))
                    }

                    val graph = Graph(vertices = verticesMap, edges = edgesList)

                    // Генерируем шаги для анимации
                    val response = graph.generateVisualSteps()

                    // Отправляем ответ обратно на фронтенд
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Unknown error")))
                }
            }
        }
    }.start(wait = true)
}