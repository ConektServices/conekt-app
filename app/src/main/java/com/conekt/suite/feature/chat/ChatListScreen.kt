package com.conekt.suite.feature.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatListScreen(
    onOpenThread:  (convId: String, otherId: String, name: String, avatar: String?) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    vm: ChatListViewModel = viewModel()
) {
    val state  by vm.state.collectAsState()
    val scope  = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF08090D))
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Status bar spacer ─────────────────────────────────────────────
            Spacer(Modifier.statusBarsPadding())

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("your circle", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.40f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChatHeaderBtn(Icons.Rounded.Refresh)    { vm.refresh() }
                    ChatHeaderBtn(Icons.Rounded.PersonAdd)  {}
                    ChatHeaderBtn(Icons.Rounded.ModeEdit) {}
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                shape    = RoundedCornerShape(18.dp),
                color    = Color.White.copy(alpha = 0.07f)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value         = state.searchQuery,
                        onValueChange = vm::onSearchQueryChange,
                        singleLine    = true,
                        textStyle     = TextStyle(color = Color.White, fontSize = 15.sp),
                        modifier      = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Search people…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.32f))
                            }
                            inner()
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.size(16.dp).clickable { vm.clearSearch() })
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Content ───────────────────────────────────────────────────────
            if (state.searchQuery.isNotEmpty()) {
                // Search results
                if (state.isSearching) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                    }
                } else if (state.searchResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found", color = Color.White.copy(alpha = 0.40f), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(state.searchResults, key = { it.id }) { user ->
                            PeopleSearchRow(user = user, onTap = {
                                // Tap → open profile (which has message button)
                                onOpenProfile(user.id)
                            })
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Stories strip
                    if (state.stories.isNotEmpty()) {
                        item(key = "stories") {
                            ChatStoriesStrip(stories = state.stories)
                            Spacer(Modifier.height(18.dp))
                        }
                    }

                    // Section header
                    if (state.conversations.isNotEmpty()) {
                        item(key = "msg_header") {
                            Text(
                                "Messages",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (state.isLoading) {
                        item(key = "loading") {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else if (state.conversations.isEmpty()) {
                        item(key = "empty") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(52.dp))
                                    Text("No conversations yet", color = Color.White.copy(alpha = 0.50f), style = MaterialTheme.typography.titleSmall)
                                    Text("Search for people to start chatting", color = Color.White.copy(alpha = 0.28f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else {
                        items(state.conversations, key = { it.id }) { conv ->
                            ConversationListRow(conv = conv, onClick = {
                                onOpenThread(conv.id, conv.otherUserId, conv.name ?: "Chat", conv.avatarUrl)
                            })
                        }
                    }
                }
            }
        }
    }
}

// ── Stories strip ─────────────────────────────────────────────────────────────

@Composable
private fun ChatStoriesStrip(stories: List<StoryPreview>) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Add story button
        item(key = "add_story") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                Box(
                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Start a story", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1)
            }
        }

        items(stories.distinctBy { it.authorId }.take(12), key = { it.id }) { story ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                Box(
                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)))
                        .padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF08090D)).padding(2.dp)) {
                        if (story.avatarUrl != null) {
                            AsyncImage(story.avatarUrl, null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(BrandEnd.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                Text(story.authorName.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(story.authorName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Conversation row ──────────────────────────────────────────────────────────

@Composable
private fun ConversationListRow(conv: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
                conv.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            (conv.name ?: "?").first().uppercaseChar().toString(),
                            color      = BrandEnd,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.titleMedium
                        )
                    }
            }
            if (conv.isOnline) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF2BD96B)).border(2.dp, Color(0xFF08090D), CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(conv.name ?: "Chat", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                conv.lastMessage ?: "Start a conversation",
                style    = MaterialTheme.typography.bodySmall,
                color    = if (conv.unreadCount > 0) Color.White.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.40f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            conv.lastMessageAt?.let {
                Text(chatFormatTime(it), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.30f))
            }
            if (conv.unreadCount > 0) {
                Box(Modifier.defaultMinSize(minWidth = 20.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                    Text("${conv.unreadCount}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ── People search row ─────────────────────────────────────────────────────────

@Composable
private fun PeopleSearchRow(user: UserSearchResult, onTap: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onTap() }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
            user.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text((user.displayName ?: user.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.displayName ?: user.username, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.40f))
        }
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.07f)) {
            Text("View", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeaderBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

internal fun chatFormatTime(iso: String): String {
    return try {
        val inst = Instant.parse(iso)
        val zdt  = inst.atZone(ZoneId.systemDefault())
        val now  = java.time.ZonedDateTime.now()
        if (zdt.toLocalDate() == now.toLocalDate())
            zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
        else
            zdt.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (_: Exception) { "" }
}