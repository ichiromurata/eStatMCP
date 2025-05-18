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
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.*

fun configureServer(): Server {
    // Base URL for the e-Stat API
    val baseUrl = "https://api.e-stat.go.jp"

    // Create an HTTP client with a default request configuration and XML content negotiation
    val httpClient = HttpClient {
        defaultRequest {
            url(baseUrl)
            headers {
                append("Accept", "application/xml")
                append("Accept-Encoding", "gzip")
                append("User-Agent", "eStatMCPServer/0.2")
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
            version = "0.2.0" // Version of the implementation
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    // Register a tool to fetch a list of table
    server.addTool(
        name = "get_tables",
        description = """
            Searches statistics tables from the portal site of official statistics of Japan, e-Stat.
            Use this to get a list of table name and ID.
            Note that monthly surveys don't have reference period, so you don't have to specify one for those surveys.
            Some tables have the same name but different IDs. Maybe their metadata could differ. 
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
                            "description" to JsonPrimitive("5 digits for ministry code or 8 digits for survey ID")
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
//                    // Pending
//                    "collectArea" to JsonObject(
//                        mapOf(
//                            "type" to JsonPrimitive("string"),
//                            "description" to JsonPrimitive("1: All Japan, 2: By prefectures, 3: By municipalities")
//                        )
//                    ),
                    "startPosition" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Skip the first results and start from the specified position.")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Max number of results per request.")
                        )
                    ),
                    "updatedDate" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("When the results were updated specified in one of the following formats: yyyy, yyyymm, yyyymm-yyyymm")
                        )
                    ),
                )
            )
        )
    ) { request ->
        val reqArgs = request.arguments.mapValues {
            val valueContent = it.value.jsonPrimitive.content
            // Reformat surveyYears
            if (it.key.endsWith("Years") && valueContent.matches(Regex("^\\d{4}-\\d{4}$"))) {
                valueContent.replace("-", "01-") + "12"
            }
            else {
                valueContent
            }
        }

        val (isError, text, res) = httpClient.getTables(reqArgs)

        if(res == null) {
            CallToolResult(isError = isError, content = listOf(TextContent(text)))
        }
        else {
            CallToolResult(isError = isError, content = listOf(TextContent(text),
                    EmbeddedResource(TextResourceContents(res, "", "application/json"))))
        }
    }

    // Register a tool to fetch a list of table
    server.addTool(
        name = "get_surveys",
        description = """
            Searches surveys that results are available to get from the portal site of official statistics of Japan, e-Stat.
            Use this when you are unsure which survey is appropriate for the context.
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
                            "description" to JsonPrimitive("5 digits for ministry code or 8 digits for survey ID")
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
                    "startPosition" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Skip the first results and start from the specified position.")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Max number of results per request.")
                        )
                    ),
                    "updatedDate" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("When the results were updated specified in one of the following formats: yyyy, yyyymm, yyyymm-yyyymm")
                        )
                    ),
                )
            )
        )
    ) { request ->
        val reqArgs = request.arguments.mapValues {
            val valueContent = it.value.jsonPrimitive.content
            // Reformat surveyYears
            if (it.key.endsWith("Years") && valueContent.matches(Regex("^\\d{4}-\\d{4}$"))) {
                valueContent.replace("-", "01-") + "12"
            }
            else {
                valueContent
            }
        }

        val (isError, text, res) = httpClient.getSurveys(reqArgs)

        if(res == null) {
            CallToolResult(isError = isError, content = listOf(TextContent(text)))
        }
        else {
            CallToolResult(isError = isError, content = listOf(TextContent(text),
                EmbeddedResource(TextResourceContents(res, "", "application/json"))))
        }
    }

    // Register a tool to fetch metadata of specific table
    server.addTool(
        name = "get_metadata",
        description = """
            Gets the metadata of the requested table. If you don't know the ID, use `get_tables` beforehand.
            Result will be truncated for over 10 codes per classification item.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "statsDataId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Table ID")
                        )
                    )
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

        val (isError, text, res) = httpClient.getMetadata(reqArgs)

        if(res == null) {
            CallToolResult(isError = isError, content = listOf(TextContent(text)))
        }
        else {
            CallToolResult(isError = isError, content = listOf(TextContent(text),
                EmbeddedResource(TextResourceContents(res, "", "application/json"))))
        }
    }

    // Register a tool to fetch statistical data
    server.addTool(
        name = "get_data",
        description = """
            Gets the data of the requested table. If you don't know the ID, use `get_tables` beforehand.
            Use metadata to filter the result.
            Some result values have special annotation format like '100 <A1>'.
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
                    "cdCat01" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat01-code")
                        )
                    ),
                    "cdCat02" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat02-code")
                        )
                    ),
                    "cdCat03" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat03-code")
                        )
                    ),
                    "cdCat04" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat04-code")
                        )
                    ),
                    "cdCat05" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat05-code")
                        )
                    ),
                    "cdCat06" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat06-code")
                        )
                    ),
                    "cdCat07" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat07-code")
                        )
                    ),
                    "cdCat08" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat08-code")
                        )
                    ),
                    "cdCat09" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Filter by cat09-code")
                        )
                    ),
                    "startPosition" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Skip the first results and start from the specified position.")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Max number of results per request.")
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

        val (isError, text, res, extRes) = httpClient.getData(reqArgs)

        if(res == null) {
            CallToolResult(isError = isError, content = listOf(TextContent(text)))
        }
        else if(extRes == null) {
            CallToolResult(isError = isError, content = listOf(TextContent(text),
                EmbeddedResource(TextResourceContents(res, "", "application/json"))))
        } else {
            CallToolResult(isError = isError, content = listOf(TextContent(text),
                EmbeddedResource(TextResourceContents(res, "", "application/json")),
                EmbeddedResource(TextResourceContents(extRes, "", "application/json"))))
        }
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
        server.onClose {
            done.complete()
        }
        done.join()
    }
}
