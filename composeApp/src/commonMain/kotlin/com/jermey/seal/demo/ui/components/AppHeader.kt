package com.jermey.seal.demo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import seal.composeapp.generated.resources.Res
import seal.composeapp.generated.resources.logo

/**
 * App header with logo, title, subtitle and info button.
 */
@Composable
fun AppHeader(
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Seal Logo",
                modifier = Modifier.size(120.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Seal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Certificate Transparency",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Verify CT compliance of HTTPS connections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "About",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
