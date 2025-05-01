package space.resolvingcode.eStatMCP

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.*

fun configureServer(): Server {
    val def = CompletableDeferred<Unit>()

    // Base URL for the e-Stat API
    val baseUrl = "https://api.e-stat.go.jp"

    // Create an HTTP client with a default request configuration and XML content negotiation
    val httpClient = HttpClient {
        defaultRequest {
            url(baseUrl)
            headers {
                append("Accept", "application/xml")
                append("Accept-Encoding", "gzip")
                append("User-Agent", "eStatApiAgent/0.1")
            }
            contentType(ContentType.Application.Xml)
        }
        // Install content negotiation plugin for XML serialization/deserialization
        install(ContentNegotiation) {
            xml()
        }
        // Install Timeout plugin
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
        }
    }

    // Create the MCP Server instance with a basic implementation
    val server = Server(
        Implementation(
            name = "e-StatJP", // Tool name is "e-StatJP"
            version = "0.1.0" // Version of the implementation
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    ) { def.complete(Unit) }

    // Register a tool to fetch a list of table
    server.addTool(
        name = "get_tables",
        description = """
            Get a list of table.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "lang" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Language (J or E)")
                        )
                    ),
                    "surveyYears" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The period referenced by the survey specified in one of the following formats: yyyy, yyyymm, yyyymm-yyyymm")
                        )
                    ),
                    "openYears" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("When the statistics were disseminated specified in one of the following formats: yyyy, yyyymm, yyyymm-yyyymm")
                        )
                    ),
                    "statsField" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("2 or 4 digits code for statistics classification")
                        )
                    ),
                    "statsCode" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("5 digits for ministry code or 8 digits for statistical table ID")
                        )
                    ),
                    "searchWord" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Search words. Multiple words should be connected with AND or OR.")
                        )
                    ),
                    "searchKind" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("1: General statistics, 2: Small area or mesh statistics for Census")
                        )
                    ),
                    "collectArea" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("1: All Japan, 2: By prefectures, 3: By municipalities")
                        )
                    ),
                    "statsNameList" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Y: Get only the names of statistics instead of the data")
                        )
                    ),
                    "startPosition" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Skip specified number of rows of the response")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Max number of rows of the response. Default is 100000.")
                        )
                    ),
                    "updatedDate" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("When the results were updated specified in one of the following formats: yyyy, yyyymm, yyyymm-yyyymm")
                        )
                    ),
                )
            ),
            required = listOf("surveyYears")
        )
    ) { request ->
        val reqArgs = request.arguments.mapValues {
            val valueContent = it.value.jsonPrimitive.content
            // Reformat surveyYears
            if (it.key == "surveyYears" && valueContent.matches(Regex("^\\d{4}-\\d{4}$"))) {
                valueContent.replace("-", "01-") + "12"
            }
            else {
                valueContent
            }
        }

        val (isError, tables) = httpClient.getTables(reqArgs)

        CallToolResult(isError = isError, content = tables.map { TextContent(it) })
    }

    // Register a tool to fetch statistical data
    server.addTool(
        name = "get_data",
        description = """
            Get data of a table specified by corresponding table ID. If you don't know the ID, use `get_tables` beforehand.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "statsDataId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Table ID")
                        )
                    ),
                    "cdTab" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by tab-code")
                        )
                    ),
                    "cdTime" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by time-code")
                        )
                    ),
                    "cdArea" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by area-code")
                        )
                    ),
                    "startPosition" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Skip specified number of rows of the response")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Max number of rows of the response. Default is 100000.")
                        )
                    ),
                    "metaGetFlg" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Get description for metadata codes? (Y or N)")
                        )
                    ),
                    "annotationGetFlg" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Get description for non-numeric values? (Y or N)")
                        )
                    ),
                )
            ),
            required = listOf("statsDataId")
        )
    ) { request ->
        val reqArgs = request.arguments.mapValues {
            it.value.jsonPrimitive.content
        }

        if (!reqArgs.containsKey("statsDataId")) {
            return@addTool CallToolResult(
                content = listOf(TextContent("The 'statsDataId' parameter is required."))
            )
        }

        val (isError, statsData) = httpClient.getData(reqArgs)

        CallToolResult(isError = isError, content = statsData.map { TextContent(it) })
    }

    return server
}

// Main function to run the MCP server
fun runMcpServerUsingStdio() {
    val server = configureServer()

    // Create a transport using standard IO for server communication
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onCloseCallback = {
            done.complete()
        }
        done.join()
    }
}
