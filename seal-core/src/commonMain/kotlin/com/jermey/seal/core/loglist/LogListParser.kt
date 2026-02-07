package com.jermey.seal.core.loglist

import com.jermey.seal.core.model.LogOperator
import com.jermey.seal.core.model.LogServer
import kotlinx.serialization.json.Json

/**
 * Parses the Google CT Log List V3 JSON format.
 * Uses lenient/defensive configuration to tolerate unknown fields, empty arrays, etc.
 */
public object LogListParser {
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Parse a V3 log list JSON string into the internal DTO.
     * @param jsonString The raw JSON content
     * @return Parsed DTO, never throws on well-formed JSON with unknown fields
     */
    internal fun parse(jsonString: String): LogListDto {
        return json.decodeFromString<LogListDto>(jsonString)
    }

    /**
     * Parse a V3 log list JSON string and map to [LogOperator] domain models.
     * @param jsonString The raw JSON content
     * @return List of operators with their associated log servers
     */
    public fun parseToOperators(jsonString: String): List<LogOperator> {
        return LogListMapper.mapToOperators(parse(jsonString))
    }

    /**
     * Parse a V3 log list JSON string and map to a flat list of [LogServer]s.
     * @param jsonString The raw JSON content
     * @return Flat list of all log servers from all operators
     */
    public fun parseToLogServers(jsonString: String): List<LogServer> {
        return LogListMapper.mapToLogServers(parse(jsonString))
    }
}
