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
    onOpenThread:  (convId: String, otherId: String, name: String, avatar: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    vm: ChatListViewModel = viewModel()
) {
    val state = vm.state.collectAsState().value
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(Color(0xFF09090F))) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.statusBarsPadding())

            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("your circle", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.38f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeaderIcon(Icons.Rounded.Refresh) { vm.load() }
                    HeaderIcon(Icons.Rounded.EditNote) { }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value         = state.query,
                        onValueChange = vm::onQueryChange,
                        singleLine    = true,
                        textStyle     = TextStyle(color = Color.White, fontSize = 15.sp),
                        modifier      = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.query.isEmpty()) Text("Search people…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.30f))
                            inner()
                        }
                    )
                    if (state.query.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.38f),
                            modifier = Modifier.size(16.dp).clickable { vm.clearQuery() })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Body ──────────────────────────────────────────────────────────
            if (state.query.isNotEmpty()) {
                // Search results
                SearchResults(
                    results    = state.searchResults,
                    isSearching = state.isSearching,
                    onOpenProfile = onOpenProfile,
                    onMessage  = { user ->
                        scope.launch {
                            val convId = vm.openOrCreateDm(user.id)
                            if (convId.isNotBlank()) {
                                onOpenThread(convId, user.id, user.displayName ?: user.username, user.avatarUrl ?: "")
                            }
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Stories strip
                    if (state.stories.isNotEmpty()) {
                        item(key = "stories") {
                            StoriesRow(state.stories)
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // Messages section header
                    item(key = "header") {
                        Text(
                            "Messages",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    if (state.isLoading) {
                        item(key = "loading") {
                            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else if (state.conversations.isEmpty()) {
                        item(key = "empty") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 52.dp, horizontal = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(52.dp))
                                Text("No conversations yet", color = Color.White.copy(alpha = 0.45f), style = MaterialTheme.typography.titleSmall)
                                Text("Search for people above to start chatting", color = Color.White.copy(alpha = 0.26f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        items(state.conversations, key = { it.id }) { conv ->
                            ConvRow(conv = conv, onClick = {
                                onOpenThread(conv.id, conv.otherUserId, conv.name, conv.avatarUrl ?: "")
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
private fun StoriesRow(stories: List<StoryThumb>) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "add_story") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Your story", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f), maxLines = 1)
            }
        }
        items(stories.distinctBy { it.authorId }.take(12), key = { it.id }) { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))).padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF09090F)).padding(2.dp)) {
                        s.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) }
                            ?: Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(BrandEnd.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                                Text(s.authorName.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(s.authorName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Conversation row ──────────────────────────────────────────────────────────

@Composable
private fun ConvRow(conv: ConversationItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
                conv.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(conv.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
            }
            if (conv.isOnline) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF2BD96B)).border(2.dp, Color(0xFF09090F), CircleShape))
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(conv.name, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                conv.lastMessage ?: "Tap to start chatting",
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.White.copy(alpha = if (conv.unreadCount > 0) 0.80f else 0.38f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            conv.lastMessageAt?.let { Text(fmtTime(it), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.28f)) }
            if (conv.unreadCount > 0) {
                Box(Modifier.defaultMinSize(20.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                    Text("${conv.unreadCount}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ── Search results ────────────────────────────────────────────────────────────

@Composable
private fun SearchResults(
    results: List<UserSearchResult>,
    isSearching: Boolean,
    onOpenProfile: (String) -> Unit,
    onMessage: (UserSearchResult) -> Unit
) {
    if (isSearching) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        }
        return
    }
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found", color = Color.White.copy(alpha = 0.38f), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
        items(results, key = { it.id }) { user ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenProfile(user.id) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.14f))) {
                    user.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text((user.displayName ?: user.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                        }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.displayName ?: user.username, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.38f))
                }
                // Message button
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onMessage(user) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Send, "Message", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

private fun fmtTime(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    if (zdt.toLocalDate() == now.toLocalDate()) zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    else zdt.format(DateTimeFormatter.ofPattern("MMM d"))
}.getOrDefault("")