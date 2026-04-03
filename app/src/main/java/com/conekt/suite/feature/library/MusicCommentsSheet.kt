package com.conekt.suite.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.MusicCommentWithAuthor
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.ConektGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicCommentsSheet(
    state: CommentsSheetState,
    onDraftChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.visible) return
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color(0xFF0D0E14),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Comments",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint               = Color.White.copy(alpha = 0.56f),
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Comments list
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color    = BrandEnd, strokeWidth = 2.dp
                        )
                    }
                    state.comments.isEmpty() -> {
                        Column(
                            modifier            = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "💬", style = MaterialTheme.typography.displaySmall)
                            Text(
                                text  = "No comments yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                            Text(
                                text  = "Be the first to comment",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.30f)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.comments, key = { it.id }) { comment ->
                                CommentRow(comment)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Input row
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value         = state.draftText,
                    onValueChange = onDraftChange,
                    placeholder   = { Text("Add a comment...", color = Color.White.copy(alpha = 0.28f)) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(20.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { focusManager.clearFocus(); onPostComment() }
                    ),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = BrandEnd,
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White.copy(alpha = 0.88f)
                    )
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.draftText.isNotBlank()) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
                        )
                        .clickable(enabled = state.draftText.isNotBlank() && !state.isPosting) {
                            focusManager.clearFocus()
                            onPostComment()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isPosting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector        = Icons.Rounded.Send,
                            contentDescription = "Post",
                            tint               = if (state.draftText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.28f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: MusicCommentWithAuthor) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        val avatarUrl = comment.author.avatarUrl?.ifBlank { null }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(com.conekt.suite.ui.theme.BrandStart, BrandEnd)
                    )
                )
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model              = avatarUrl,
                    contentDescription = comment.author.username,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = comment.author.displayName ?: comment.author.username,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text  = timeAgo(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.36f)
                )
            }
            Text(
                text     = comment.body,
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun timeAgo(iso: String): String {
    return try {
        val then   = java.time.Instant.parse(iso)
        val diff   = java.time.Duration.between(then, java.time.Instant.now())
        when {
            diff.toMinutes() < 1  -> "now"
            diff.toHours()   < 1  -> "${diff.toMinutes()}m"
            diff.toDays()    < 1  -> "${diff.toHours()}h"
            diff.toDays()    < 7  -> "${diff.toDays()}d"
            else                  -> "${diff.toDays() / 7}w"
        }
    } catch (e: Exception) { "" }
}