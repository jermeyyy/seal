package com.jermey.seal.core.loglist

import com.jermey.seal.core.model.LogId
import com.jermey.seal.core.model.LogOperator
import com.jermey.seal.core.model.LogServer
import com.jermey.seal.core.model.LogState
import com.jermey.seal.core.model.TemporalInterval
import kotlinx.datetime.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Maps parsed log list DTOs to domain models.
 */
internal object LogListMapper {
    /**
     * Map a parsed [LogListDto] to a list of [LogOperator]s with their [LogServer]s.
     */
    @OptIn(ExperimentalEncodingApi::class)
    internal fun mapToOperators(dto: LogListDto): List<LogOperator> {
        return dto.operators.mapNotNull { operatorDto ->
            val servers = mutableListOf<LogServer>()

            // Map regular logs
            for (log in operatorDto.logs) {
                mapLogDto(log, operatorDto.name)?.let { servers.add(it) }
            }

            // Map tiled logs
            for (tiledLog in operatorDto.tiledLogs) {
                mapTiledLogDto(tiledLog, operatorDto.name)?.let { servers.add(it) }
            }

            LogOperator(name = operatorDto.name, logs = servers)
        }
    }

    /**
     * Convenience: map to a flat list of all [LogServer]s.
     */
    internal fun mapToLogServers(dto: LogListDto): List<LogServer> {
        return mapToOperators(dto).flatMap { it.logs }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun mapLogDto(log: LogDto, operatorName: String): LogServer? {
        return try {
            val keyBytes = Base64.decode(log.key)
            val logIdBytes = Base64.decode(log.logId)
            LogServer(
                logId = LogId(logIdBytes),
                publicKey = keyBytes,
                operator = operatorName,
                url = log.url ?: "",
                state = mapState(log.state),
                temporalInterval = mapTemporalInterval(log.temporalInterval),
            )
        } catch (_: Exception) {
            null // Skip malformed entries
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun mapTiledLogDto(log: TiledLogDto, operatorName: String): LogServer? {
        return try {
            val keyBytes = Base64.decode(log.key)
            val logIdBytes = Base64.decode(log.logId)
            LogServer(
                logId = LogId(logIdBytes),
                publicKey = keyBytes,
                operator = operatorName,
                url = log.url ?: "",
                state = mapState(log.state),
                temporalInterval = mapTemporalInterval(log.temporalInterval),
            )
        } catch (_: Exception) {
            null // Skip malformed entries
        }
    }

    private fun mapState(dto: LogStateDto?): LogState {
        if (dto == null) return LogState.PENDING
        return when {
            dto.usable != null -> LogState.USABLE
            dto.qualified != null -> LogState.QUALIFIED
            dto.readOnly != null -> LogState.READ_ONLY
            dto.retired != null -> LogState.RETIRED
            dto.rejected != null -> LogState.REJECTED
            dto.pending != null -> LogState.PENDING
            else -> LogState.PENDING
        }
    }

    private fun mapTemporalInterval(dto: TemporalIntervalDto?): TemporalInterval? {
        if (dto == null) return null
        return try {
            TemporalInterval(
                startInclusive = Instant.parse(dto.startInclusive ?: return null),
                endExclusive = Instant.parse(dto.endExclusive ?: return null),
            )
        } catch (_: Exception) {
            null
        }
    }
}
