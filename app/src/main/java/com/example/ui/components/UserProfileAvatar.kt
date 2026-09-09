package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoViolet

/**
 * Reusable user profile avatar component with support for:
 * - Loaded image from URI (captured camera photo or local file)
 * - Fallback to user initials
 * - Optional camera capture badge button
 */
@Composable
fun UserProfileAvatar(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    backgroundColor: Color = BentoViolet,
    showCameraBadge: Boolean = false,
    onCameraClick: (() -> Unit)? = null
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "U" }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo of $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, backgroundColor, CircleShape)
                    .testTag("user_profile_photo_image")
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }

        // Camera overlay badge
        if (showCameraBadge && onCameraClick != null) {
            Surface(
                shape = CircleShape,
                color = BentoPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size((size * 0.38f).coerceAtLeast(26.dp))
                    .clip(CircleShape)
                    .clickable { onCameraClick() }
                    .testTag("avatar_camera_badge_btn")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Capture Profile Photo",
                        tint = Color.White,
                        modifier = Modifier.size((size * 0.22f).coerceAtLeast(14.dp))
                    )
                }
            }
        }
    }
}
