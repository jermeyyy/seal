package com.jermey.seal.core.loglist

import com.jermey.seal.core.model.LogState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class LogListParserTest {

    // 32 bytes encoded as base64 for a valid LogId
    private val validLogId = Base64.encode(ByteArray(32) { it.toByte() })

    // A short key (just needs to be valid base64)
    private val validKey = Base64.encode(ByteArray(65) { (it + 10).toByte() })

    @Test
    fun parseMinimalValidLogList() {
        val json = """
        {
            "version": "v3",
            "log_list_timestamp": "2025-01-01T00:00:00Z",
            "operators": [
                {
                    "name": "Test Operator",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://ct.example.com/log/",
                            "state": { "usable": { "timestamp": "2024-01-01T00:00:00Z" } },
                            "temporal_interval": {
                                "start_inclusive": "2024-01-01T00:00:00Z",
                                "end_exclusive": "2025-01-01T00:00:00Z"
                            }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        assertEquals("v3", dto.version)
        assertEquals(1, dto.operators.size)
        assertEquals("Test Operator", dto.operators[0].name)
        assertEquals(1, dto.operators[0].logs.size)
        assertEquals(validLogId, dto.operators[0].logs[0].logId)
    }

    @Test
    fun parseOperatorWithEmptyLogs() {
        val json = """
        {
            "operators": [
                {
                    "name": "Empty Operator",
                    "logs": [],
                    "tiled_logs": []
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        assertEquals(1, dto.operators.size)
        assertTrue(dto.operators[0].logs.isEmpty())
        assertTrue(dto.operators[0].tiledLogs.isEmpty())
    }

    @Test
    fun parseOperatorWithTiledLogsOnly() {
        val json = """
        {
            "operators": [
                {
                    "name": "Tiled Operator",
                    "tiled_logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://ct.example.com/tiled/",
                            "state": { "qualified": { "timestamp": "2024-06-01T00:00:00Z" } }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        assertEquals(1, dto.operators.size)
        assertTrue(dto.operators[0].logs.isEmpty())
        assertEquals(1, dto.operators[0].tiledLogs.size)
        assertEquals("https://ct.example.com/tiled/", dto.operators[0].tiledLogs[0].url)
    }

    @Test
    fun parseHandlesUnknownFieldsGracefully() {
        val json = """
        {
            "version": "v3",
            "some_unknown_field": 42,
            "operators": [
                {
                    "name": "Op",
                    "unknown_nested": { "x": 1 },
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "extra_field": true
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        assertEquals("v3", dto.version)
        assertEquals(1, dto.operators.size)
        assertEquals(1, dto.operators[0].logs.size)
    }

    @Test
    fun mapToLogServersCorrectly() {
        val json = """
        {
            "operators": [
                {
                    "name": "Google",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://ct.googleapis.com/logs/argon2024/",
                            "state": { "usable": { "timestamp": "2024-01-01T00:00:00Z" } },
                            "temporal_interval": {
                                "start_inclusive": "2024-01-01T00:00:00Z",
                                "end_exclusive": "2025-01-01T00:00:00Z"
                            }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val servers = LogListMapper.mapToLogServers(dto)

        assertEquals(1, servers.size)
        val server = servers[0]
        assertEquals("Google", server.operator)
        assertEquals("https://ct.googleapis.com/logs/argon2024/", server.url)
        assertEquals(LogState.USABLE, server.state)
        assertNotNull(server.temporalInterval)
        assertEquals(32, server.logId.keyId.size)
    }

    @Test
    fun mapToOperatorsIncludesTiledLogs() {
        val json = """
        {
            "operators": [
                {
                    "name": "Cloudflare",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://ct.cloudflare.com/log/",
                            "state": { "retired": { "timestamp": "2023-06-01T00:00:00Z" } }
                        }
                    ],
                    "tiled_logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://ct.cloudflare.com/tiled/",
                            "state": { "qualified": {} }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val operators = LogListMapper.mapToOperators(dto)

        assertEquals(1, operators.size)
        assertEquals("Cloudflare", operators[0].name)
        assertEquals(2, operators[0].logs.size)
        assertEquals(LogState.RETIRED, operators[0].logs[0].state)
        assertEquals(LogState.QUALIFIED, operators[0].logs[1].state)
    }

    @Test
    fun mapSkipsMalformedBase64Entries() {
        val json = """
        {
            "operators": [
                {
                    "name": "Broken",
                    "logs": [
                        {
                            "log_id": "!!!not-valid-base64!!!",
                            "key": "$validKey"
                        },
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "url": "https://good.example.com/",
                            "state": { "usable": {} }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val servers = LogListMapper.mapToLogServers(dto)

        // First entry should be skipped (malformed base64), second should succeed
        assertEquals(1, servers.size)
        assertEquals("https://good.example.com/", servers[0].url)
    }

    @Test
    fun mapStateDefaultsToPending() {
        val json = """
        {
            "operators": [
                {
                    "name": "NoState",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val servers = LogListMapper.mapToLogServers(dto)

        assertEquals(1, servers.size)
        assertEquals(LogState.PENDING, servers[0].state)
    }

    @Test
    fun mapTemporalIntervalIsNullWhenMissing() {
        val json = """
        {
            "operators": [
                {
                    "name": "NoInterval",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "state": { "usable": {} }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val servers = LogListMapper.mapToLogServers(dto)

        assertEquals(1, servers.size)
        assertNull(servers[0].temporalInterval)
    }

    @Test
    fun mapReadOnlyState() {
        val json = """
        {
            "operators": [
                {
                    "name": "ReadOnly Op",
                    "logs": [
                        {
                            "log_id": "$validLogId",
                            "key": "$validKey",
                            "state": { "readonly": { "timestamp": "2024-06-01T00:00:00Z" } }
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val dto = LogListParser.parse(json)
        val servers = LogListMapper.mapToLogServers(dto)

        assertEquals(1, servers.size)
        assertEquals(LogState.READ_ONLY, servers[0].state)
    }
}
