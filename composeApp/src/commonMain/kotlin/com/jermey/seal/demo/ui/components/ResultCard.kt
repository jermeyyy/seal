package com.jermey.seal.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.demo.CtCheckResult

/**
 * A card displaying the CT verification result for a single URL.
 */
@Composable
fun ResultCard(
    result: CtCheckResult,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (icon, iconTint) = statusIconInfo(result.status)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.url.removePrefix("https://"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusDescription(result.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class IconInfo(val icon: ImageVector, val tint: Color)

@Composable
private fun statusIconInfo(status: CtCheckResult.Status): IconInfo = when (status) {
    is CtCheckResult.Status.Pending -> IconInfo(
        Icons.Default.HourglassEmpty,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    is CtCheckResult.Status.Loading -> IconInfo(
        Icons.Default.Sync,
        MaterialTheme.colorScheme.primary,
    )
    is CtCheckResult.Status.Completed -> when (status.result) {
        is VerificationResult.Success -> IconInfo(
            Icons.Default.CheckCircle,
            Color(0xFF2E7D32),
        )
        is VerificationResult.Failure -> IconInfo(
            Icons.Default.Cancel,
            MaterialTheme.colorScheme.error,
        )
    }
    is CtCheckResult.Status.Error -> IconInfo(
        Icons.Default.Error,
        MaterialTheme.colorScheme.error,
    )
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
    is VerificationResult.Success.OsVerified ->
        "CT Verified (OS-level, ${result.platform})"
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
