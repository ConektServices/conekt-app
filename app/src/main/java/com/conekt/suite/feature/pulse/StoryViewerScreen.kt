package com.conekt.suite.feature.pulse

// ─────────────────────────────────────────────────────────────────────────────
// StoryViewerScreen.kt
//
// Full-screen story viewer. Tapping the screen or using the timer auto-advances
// through stories from the same author, then calls onClose.
//
// Usage: shown as an overlay from PulseScreen when a story ring is tapped.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.StoryWithAuthor
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import kotlinx.coroutines.delay

/** Duration each story slide is shown before auto-advancing (ms). */
private const val STORY_DURATION_MS = 5_000L

/**
 * Full-screen story viewer.
 *
 * @param stories  All stories in the current batch (same author or full feed).
 * @param startIndex  Which story to open first.
 * @param onClose  Called when the user dismisses or all stories finish.
 */
@Composable
fun StoryViewerScreen(
    stories: List<StoryWithAuthor>,
    startIndex: Int = 0,
    onClose: () -> Unit
) {
    if (stories.isEmpty()) { onClose(); return }

    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, stories.lastIndex)) }
    val story = stories[currentIndex]

    // Progress within the current story: 0f → 1f over STORY_DURATION_MS
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }

    // Tick the progress bar and auto-advance
    LaunchedEffect(currentIndex) {
        progress = 0f
        val steps = 100
        val stepMs = STORY_DURATION_MS / steps
        repeat(steps) {
            delay(stepMs)
            progress = (it + 1) / steps.toFloat()
        }
        // Auto-advance to next story or close
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap left third → previous, tap right two thirds → next
            .pointerInput(currentIndex) {
                detectTapGestures { offset ->
                    if (offset.x < size.width * 0.33f) {
                        if (currentIndex > 0) currentIndex-- else onClose()
                    } else {
                        if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                    }
                }
            }
    ) {
        // ── Story media ───────────────────────────────────────────────────────
        AsyncImage(
            model              = story.mediaUrl,
            contentDescription = "Story",
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )

        // Top gradient for progress bars / header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient for caption readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                    )
                )
        )

        // ── Progress bars ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val barProgress = when {
                        idx < currentIndex  -> 1f
                        idx == currentIndex -> progress
                        else                -> 0f
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = barProgress,
                        animationSpec = if (idx == currentIndex) tween(0) else tween(200),
                        label = "bar_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.30f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(3.dp)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Author header ─────────────────────────────────────────────────
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.spacedBy(10.dp),
                modifier               = Modifier.fillMaxWidth()
            ) {
                // Avatar ring
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(BrandStart, BrandEnd)),
                            CircleShape
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                    ) {
                        val avatar = story.author.avatarUrl?.ifBlank { null }
                        if (avatar != null) {
                            AsyncImage(
                                model              = avatar,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            val name = story.author.displayName ?: story.author.username
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(BrandEnd.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name.first().uppercaseChar().toString(),
                                    color      = BrandEnd,
                                    fontWeight = FontWeight.Bold,
                                    style      = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text       = story.author.displayName ?: story.author.username,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text  = "@${story.author.username}",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // More options (placeholder)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.30f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Close button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.30f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Caption ───────────────────────────────────────────────────────────
        story.caption?.ifBlank { null }?.let { caption ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text  = caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}