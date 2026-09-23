package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * High-fidelity, professional CircularProgressIndicator loading components
 * tailored for dashboard network requests and cloud synchronization operations.
 */

/**
 * Animated banner displayed at the top of dashboards when background or pull-to-refresh
 * data fetching operations are actively communicating with Cloud Firestore.
 */
@Composable
fun DashboardSyncBanner(
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Synchronizing with Server...",
    subtitle: String? = "Fetching real-time updates from Cloud Firestore",
    accentColor: Color = BentoPrimary,
    testTag: String = "dashboard_sync_banner"
) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(200)),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(14.dp)
        val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
        val badgeAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.2f),
                            accentColor.copy(alpha = 0.4f)
                        )
                    ),
                    shape = shape
                )
                .testTag(testTag),
            shape = shape,
            color = accentColor.copy(alpha = 0.08f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CircularProgressIndicator with glowing backdrop
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("${testTag}_spinner"),
                        strokeWidth = 2.5.dp,
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
                    )
                }

                // Text status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Live status chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f * badgeAlpha)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = accentColor
                        )
                        Text(
                            text = "SYNCING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prominent card with CircularProgressIndicator for dashboard content panels, lists,
 * or charts that are waiting for network responses.
 */
@Composable
fun DashboardDataLoadingCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "Fetching data over secure connection...",
    accentColor: Color = BentoPrimary,
    minHeight: Dp = 160.dp,
    testTag: String = "dashboard_data_loading_card"
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .testTag(testTag),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant layered CircularProgressIndicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("${testTag}_spinner"),
                    strokeWidth = 3.5.dp,
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.18f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accentColor
                )
                Text(
                    text = "Cloud Firestore Request Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Compact inline loading indicator with CircularProgressIndicator for headers,
 * list controls, or status footers.
 */
@Composable
fun DashboardInlineLoadingIndicator(
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = BentoPrimary,
    spinnerSize: Dp = 14.dp,
    testTag: String = "dashboard_inline_loading"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(spinnerSize),
            strokeWidth = 2.dp,
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.2f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
    }
}
