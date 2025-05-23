package space.resolvingcode.eStatMCP

/*
MCP Server for e-Stat Japan API

Copyright (c) 2024 Anthropic, PBC (Original sample programs)
Copyright (c) 2025 Ichiro Murata (Created based on the original project)
*/

val responseSize: String? = System.getenv("RESPONSE_SIZE") ?: null

fun main() = runMcpServerUsingStdio()
