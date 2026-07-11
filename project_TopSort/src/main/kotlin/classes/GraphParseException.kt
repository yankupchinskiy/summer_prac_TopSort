
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

package classes

class GraphParseException(message: String) : Exception(message)

@Serializable
private data class GraphPayload(
    val vertices: List<String>,
    val edges: List<List<String>>
)