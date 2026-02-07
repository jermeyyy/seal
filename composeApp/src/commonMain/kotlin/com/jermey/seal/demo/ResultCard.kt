package com.jermey.seal.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.seal.core.model.VerificationResult

/**
 * A card displaying the CT verification result for a single URL.
 */
@Composable
fun ResultCard(result: CtCheckResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result.status) {
                is CtCheckResult.Status.Completed -> {
                    when (result.status.result) {
                        is VerificationResult.Success -> MaterialTheme.colorScheme.secondaryContainer
                        is VerificationResult.Failure -> MaterialTheme.colorScheme.errorContainer
                    }
                }
                is CtCheckResult.Status.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = statusIcon(result.status),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.url.removePrefix("https://"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusDescription(result.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun statusIcon(status: CtCheckResult.Status): String = when (status) {
    is CtCheckResult.Status.Pending -> "⏳"
    is CtCheckResult.Status.Loading -> "🔄"
    is CtCheckResult.Status.Completed -> when (status.result) {
        is VerificationResult.Success -> "✅"
        is VerificationResult.Failure -> "❌"
    }
    is CtCheckResult.Status.Error -> "⚠️"
}

private fun statusDescription(status: CtCheckResult.Status): String = when (status) {
    is CtCheckResult.Status.Pending -> "Waiting…"
    is CtCheckResult.Status.Loading -> "Checking CT compliance…"
    is CtCheckResult.Status.Completed -> describeResult(status.result)
    is CtCheckResult.Status.Error -> status.message
}

private fun describeResult(result: VerificationResult): String = when (result) {
    is VerificationResult.Success.Trusted ->
        "CT Verified — ${result.validScts.size} valid SCTs"
    is VerificationResult.Success.InsecureConnection ->
        "Insecure connection (no TLS)"
    is VerificationResult.Success.DisabledForHost ->
        "CT disabled for this host"
    is VerificationResult.Success.DisabledStaleLogList ->
        "CT disabled — stale log list"
    is VerificationResult.Failure.NoScts ->
        "No SCTs found"
    is VerificationResult.Failure.TooFewSctsTrusted ->
        "Too few trusted SCTs (${result.found}/${result.required})"
    is VerificationResult.Failure.TooFewDistinctOperators ->
        "Too few distinct operators (${result.found}/${result.required})"
    is VerificationResult.Failure.LogServersFailed ->
        "Log server verification failed"
    is VerificationResult.Failure.UnknownError ->
        "Error: ${result.cause.message ?: "Unknown"}"
}
