package com.jermey.seal.core.loglist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LogListDto(
    val version: String? = null,
    @SerialName("log_list_timestamp") val logListTimestamp: String? = null,
    val operators: List<OperatorDto> = emptyList(),
)

@Serializable
internal data class OperatorDto(
    val name: String,
    val email: List<String> = emptyList(),
    val logs: List<LogDto> = emptyList(),
    @SerialName("tiled_logs") val tiledLogs: List<TiledLogDto> = emptyList(),
)

@Serializable
internal data class LogDto(
    val description: String? = null,
    @SerialName("log_id") val logId: String, // base64
    val key: String, // base64
    val url: String? = null,
    val mmd: Long? = null,
    val state: LogStateDto? = null,
    @SerialName("temporal_interval") val temporalInterval: TemporalIntervalDto? = null,
)

@Serializable
internal data class TiledLogDto(
    val description: String? = null,
    @SerialName("log_id") val logId: String, // base64
    val key: String, // base64
    val url: String? = null,
    val mmd: Long? = null,
    val state: LogStateDto? = null,
    @SerialName("temporal_interval") val temporalInterval: TemporalIntervalDto? = null,
)

@Serializable
internal data class LogStateDto(
    val pending: StateTimestampDto? = null,
    val qualified: StateTimestampDto? = null,
    val usable: StateTimestampDto? = null,
    @SerialName("readonly") val readOnly: StateTimestampDto? = null,
    val retired: StateTimestampDto? = null,
    val rejected: StateTimestampDto? = null,
)

@Serializable
internal data class StateTimestampDto(
    val timestamp: String? = null,
)

@Serializable
internal data class TemporalIntervalDto(
    @SerialName("start_inclusive") val startInclusive: String? = null,
    @SerialName("end_exclusive") val endExclusive: String? = null,
)
