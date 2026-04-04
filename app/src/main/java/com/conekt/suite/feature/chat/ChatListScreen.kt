package com.conekt.suite.feature.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatListScreen(
    onOpenThread:   (convId: String, otherId: String, name: String, avatar: String?) -> Unit,
    onOpenProfile:  (userId: String) -> Unit,
    vm: ChatListViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("your circle", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
                }
                Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassCircle(Icons.Rounded.PersonSearch) {}
                    GlassCircle(Icons.Rounded.Edit) {}
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                shape    = RoundedCornerShape(20.dp),
                color    = Color.White.copy(alpha = 0.07f)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value         = state.searchQuery,
                        onValueChange = vm::onSearchQueryChange,
                        singleLine    = true,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier      = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Search people", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.35f))
                            }
                            inner()
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(16.dp).clickable { vm.clearSearch() })
                    } else {
                        Icon(Icons.Rounded.Tune, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Search results ────────────────────────────────────────────────
            AnimatedVisibility(state.searchQuery.isNotEmpty()) {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (state.searchResults.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text("No users found", color = Color.White.copy(alpha = 0.40f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        items(state.searchResults, key = { it.id }) { user ->
                            PeopleRow(user = user, onClick = { onOpenProfile(user.id) })
                        }
                    }
                }
                return@AnimatedVisibility
            }

            // ── Stories strip ─────────────────────────────────────────────────
            if (state.stories.isNotEmpty() && state.searchQuery.isEmpty()) {
                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.padding(bottom = 16.dp)
                ) {
                    // "Start a story" button
                    item {
                        StoryAddButton()
                    }
                    items(state.stories.distinctBy { it.authorId }.take(10), key = { it.id }) { story ->
                        StoryAvatar(story = story)
                    }
                }
            }

            // ── Conversations ─────────────────────────────────────────────────
            if (state.searchQuery.isEmpty()) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier            = Modifier.fillMaxSize()
                    ) {
                        if (state.conversations.isNotEmpty()) {
                            item {
                                Text("Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            items(state.conversations, key = { it.id }) { conv ->
                                ConversationRow(conv = conv, onClick = {
                                    onOpenThread(conv.id, conv.otherUserId ?: "", conv.name ?: "Chat", conv.avatarUrl)
                                })
                            }
                        } else {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
                                        Text("No messages yet", color = Color.White.copy(alpha = 0.50f), style = MaterialTheme.typography.titleSmall)
                                        Text("Search for people to start a conversation", color = Color.White.copy(alpha = 0.30f), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Story components ──────────────────────────────────────────────────────────

@Composable
private fun StoryAddButton() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(2.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Your story", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1)
    }
}

@Composable
private fun StoryAvatar(story: StoryItem) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier.size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)))
                .padding(2.dp)
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF0A0A0E)).padding(2.dp)) {
                if (story.avatarUrl != null) {
                    AsyncImage(story.avatarUrl, null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(BrandEnd.copy(alpha = 0.30f)), contentAlignment = Alignment.Center) {
                        Text(story.authorName.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(story.authorName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.80f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Conversation row ──────────────────────────────────────────────────────────

@Composable
private fun ConversationRow(conv: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.18f))) {
                conv.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((conv.name ?: "?").first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
            }
            if (conv.isOnline) Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF2BD96B)).border(2.dp, Color(0xFF0A0A0E), CircleShape))
        }

        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(conv.name ?: "Unknown", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(conv.lastMessage ?: "Start a conversation", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            conv.lastMessageAt?.let { Text(formatTime(it), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f)) }
            if (conv.unreadCount > 0) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                    Text("${conv.unreadCount}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── People search row ─────────────────────────────────────────────────────────

@Composable
private fun PeopleRow(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.18f))) {
            user.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text((user.displayName ?: user.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(user.displayName ?: user.username, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
        }
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f)) {
            Text("View", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.70f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun GlassCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun formatTime(iso: String): String {
    return try {
        val inst = Instant.parse(iso)
        val zdt  = inst.atZone(ZoneId.systemDefault())
        val now  = java.time.ZonedDateTime.now()
        if (zdt.toLocalDate() == now.toLocalDate()) zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
        else zdt.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (_: Exception) { "" }
}

@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = singleLine,
        textStyle     = textStyle,
        modifier      = modifier,
        decorationBox = decorationBox
    )
}
