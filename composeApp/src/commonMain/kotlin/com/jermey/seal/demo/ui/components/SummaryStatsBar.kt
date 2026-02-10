package com.jermey.seal.demo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.demo.CtCheckResult

/**
 * A compact stats bar showing pass/fail/error counts after test completion.
 */
@Composable
fun SummaryStatsBar(results: List<CtCheckResult>) {
    val passed = results.count { check ->
        val s = check.status
        s is CtCheckResult.Status.Completed && s.result is VerificationResult.Success
    }
    val failed = results.count { check ->
        val s = check.status
        (s is CtCheckResult.Status.Completed && s.result is VerificationResult.Failure) ||
            s is CtCheckResult.Status.Error
    }
    val pending = results.size - passed - failed

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(
                icon = Icons.Default.CheckCircle,
                label = "Passed",
                count = passed,
                tint = Color(0xFF2E7D32),
            )
            StatItem(
                icon = Icons.Default.Cancel,
                label = "Failed",
                count = failed,
                tint = MaterialTheme.colorScheme.error,
            )
            if (pending > 0) {
                StatItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Pending",
                    count = pending,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
