# e-Stat MCP Server

An MCP server that allows generative AI to handle the [API functions](https://www.e-stat.go.jp/api/en) of the [Portal Site of Official Statistics of Japan (e-Stat)](https://www.e-stat.go.jp/).

## About MCP
Model Context Protocol (MCP) is a format proposed by Anthropic in November 2024 to inform generative AI about what an application can provide. An MCP server is implemented as a server that responds to requests from generative AI using the protocol. Many MCP servers have been created to enable generative AI to handle existing services.

## Tools
* get_tables
  - Retrieves a list of statistical tables (URL: getStatsList).

* get_surveys
  - Retrieves a list of statistical survey names instead of tables (URL: getStatsList).

* get_metadata
  - Retrieves metadata of a statistical table (URL: getMetaInfo).
  - Required parameter:
    - Statistical table ID (statsDataId).

* get_data
  - Retrieves statistical data from a table (URL: getStatsData).
  - Required parameter:
    - Statistical table ID (statsDataId).

Each function is implemented to simplify the e-Stat API response and return it as JSON format.

## Usage
### Obtaining your e-Stat Application ID
Follow the [User Guide](https://www.e-stat.go.jp/api/en/api-info/api-guide) to register as a user on e-Stat and obtain an application ID.

### Setting up Claude Desktop
If you are using Claude Desktop, add the following entry to `claude_desktop_config.json`. This file is located in `~/Library/Application Support/Claude/` on Mac or `%AppData%\Claude\` on Windows.

#### If you have built the JAR file
```json
{
  "mcpServers": {
    "eStatMCP": {
      "command": "java",
      "args": [
        "-jar",
        "build/libs/<your-jar-name>.jar"
      ],
      "env": {
        "ESTAT_API_KEY": "<Application ID>",
        "RESPONSE_SIZE": "100"
      }
    }
  }
}
```

#### Using Docker
```json
{
  "mcpServers": {
    "eStatMCP": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "ESTAT_API_KEY",
        "-e",
        "RESPONSE_SIZE",
        "ichiro21/estat-mcp"
      ],
      "env": {
        "ESTAT_API_KEY": "<Application ID>",
        "RESPONSE_SIZE": "100"
      }
    }
  }
}
```

`Application ID` should be replaced with the actual application ID you obtained from e-Stat.

The `RESPONSE_SIZE` environment variable can be adjusted to control the number of results returned by the API (default is 100,000). It enables you to control the context length of the AI chat.

### If you are using VS Code
If you are using VS Code, add the following configuration to your `settings.json`. This file can be opened by searching for "Preferences: Open User Settings (JSON)" in the Menu shown by pressing `Ctrl + Shift + P`.

#### Using Docker

```json
{
  "mcp": {
    "servers": {
      "eStatMCP": {
        "type": "stdio",
        "command": "docker",
        "args": [
          "run",
          "-i",
          "--rm",
          "-e",
          "ESTAT_API_KEY",
          "-e",
          "RESPONSE_SIZE",
          "ichiro21/estat-mcp"
        ],
        "env": {
          "ESTAT_API_KEY": "${env:ESTAT_API_KEY}",
          "RESPONSE_SIZE": "100"
        }
      }
    }
  }
}
```

In this situation, you can refer the `ESTAT_API_KEY` environment variable in your system environment variables as shown above. If you do not want to use environment variables, you can directly specify the application ID or save it in VS Code settings. (Refer to the code snippet of [this page](https://code.visualstudio.com/docs/copilot/chat/mcp-servers#_add-an-mcp-server-to-your-workspace) for more details.)

### Building the program
To build the server program, you need to have Java (17 or later) and [Gradle](https://gradle.org/install/) installed. After installing Gradle, run the following command in the root directory of this repository:

```bash
./gradlew clean build -x test
```

## Examples
Currently, I'm sharing only Japanese interactions.

### Overview of Japan's Economic Statistics
[https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21](https://claude.ai/share/ac66bee7-1178-4f8a-977c-cdd6629c0f21)

### Japan's Population Statistics
[https://claude.ai/share/5f6874e2-4fc6-42e5-a8dd-6e8feb6158df](https://claude.ai/share/5f6874e2-4fc6-42e5-a8dd-6e8feb6158df)
