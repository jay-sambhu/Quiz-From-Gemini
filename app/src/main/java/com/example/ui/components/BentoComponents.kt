package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Reusable Bento Grid Cards and Container Components
 */

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    cornerRadius: Dp = 20.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier.padding(padding),
        content = content
    )
}

@Composable
fun BentoStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    BentoCard(
        modifier = modifier,
        backgroundColor = accentColor.copy(alpha = 0.08f),
        borderColor = accentColor.copy(alpha = 0.25f),
        cornerRadius = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BentoPillTag(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun BentoSectionTitle(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.25.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
