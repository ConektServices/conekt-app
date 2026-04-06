package com.conekt.suite.feature.chat

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
import androidx.compose.ui.text.style.TextAlign
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
    onOpenThread: (convId: String, otherId: String, name: String, avatar: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    vm: ChatListViewModel = viewModel(),
    contentTopPadding: Dp = 0.dp
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF09090F))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top space
            if (contentTopPadding > 0.dp) {
                Spacer(modifier = Modifier.height(contentTopPadding))
            } else {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("your circle", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.38f))
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { vm.load() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Search bar
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = state.query,
                        onValueChange = vm::onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.query.isEmpty()) Text("Search people to chat…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.30f))
                            inner()
                        }
                    )
                    if (state.query.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(16.dp).clickable { vm.clearQuery() })
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (state.query.isNotEmpty()) {
                // ── Search results ────────────────────────────────────────────
                SearchResultsList(
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    onOpenProfile = onOpenProfile,
                    onStartChat = { user ->
                        // Launch coroutine to create/get conversation then navigate
                        scope.launch {
                            val convId = vm.openOrCreateDm(user.id)
                            if (convId.isNotBlank()) {
                                onOpenThread(
                                    convId,
                                    user.id,
                                    user.displayName ?: user.username,
                                    user.avatarUrl ?: ""
                                )
                            }
                        }
                    }
                )
            } else {
                // ── Conversation list ─────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (state.stories.isNotEmpty()) {
                        item(key = "stories") {
                            StoriesStrip(stories = state.stories)
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    item(key = "section_label") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Conversations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            if (state.conversations.isNotEmpty()) {
                                Text("${state.conversations.size}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
                            }
                        }
                    }

                    when {
                        state.isLoading -> item(key = "loading") {
                            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                        state.conversations.isEmpty() -> item(key = "empty") {
                            EmptyConversationState()
                        }
                        else -> items(state.conversations, key = { it.id }) { conv ->
                            // Each conversation row — tap to open chat thread
                            ConversationRow(
                                conv = conv,
                                onClick = {
                                    onOpenThread(conv.id, conv.otherUserId, conv.name, conv.avatarUrl ?: "")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoriesStrip(stories: List<StoryThumb>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "add_story") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp)) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Your story", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f), maxLines = 1)
            }
        }
        items(stories.distinctBy { it.authorId }.take(12), key = { it.id }) { story ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp)) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))).padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color(0xFF09090F)).padding(2.dp)) {
                        story.avatarUrl?.let {
                            AsyncImage(it, null, Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        } ?: Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(BrandEnd.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            Text(story.authorName.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(story.authorName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Conversation row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConversationRow(conv: ConversationItem, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
                    conv.avatarUrl?.ifBlank { null }?.let {
                        AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(conv.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (conv.isOnline) {
                    Box(Modifier.size(13.dp).clip(CircleShape).background(SuccessGreen).border(2.dp, Color(0xFF09090F), CircleShape))
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(Modifier.weight(1f)) {
                Text(conv.name, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    conv.lastMessage ?: "Start a conversation",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conv.unreadCount > 0) Color.White.copy(0.80f) else Color.White.copy(0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                conv.lastMessageAt?.let {
                    Text(fmtConvTime(it), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.28f))
                }
                if (conv.unreadCount > 0) {
                    Box(Modifier.defaultMinSize(minWidth = 20.dp).clip(CircleShape).background(BrandEnd), contentAlignment = Alignment.Center) {
                        Text("${conv.unreadCount}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 84.dp), color = Color.White.copy(0.05f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search results list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsList(
    results: List<UserSearchResult>,
    isSearching: Boolean,
    onOpenProfile: (String) -> Unit,
    onStartChat: (UserSearchResult) -> Unit
) {
    if (isSearching) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        }
        return
    }
    if (results.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.PersonSearch, null, tint = Color.White.copy(0.20f), modifier = Modifier.size(48.dp))
                Text("No users found", color = Color.White.copy(0.38f), style = MaterialTheme.typography.bodyMedium)
                Text("Try a different name or username", color = Color.White.copy(0.22f), style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 4.dp,
            end = 16.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(results, key = { it.id }) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(0.05f))
                    .clickable { onOpenProfile(user.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(BrandEnd.copy(0.16f))) {
                    user.avatarUrl?.ifBlank { null }?.let {
                        AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((user.displayName ?: user.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.displayName ?: user.username, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.38f))
                }
                Spacer(Modifier.width(8.dp))
                // Tap this to open/create DM and navigate to ChatThreadScreen
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ConektGradient.brandHorizontal)
                        .clickable { onStartChat(user) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("Message", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(26.dp)).background(ConektGradient.brandHorizontal), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text("No conversations yet", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Search for someone above to\nstart your first conversation", color = Color.White.copy(0.45f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

private fun fmtConvTime(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    if (zdt.toLocalDate() == now.toLocalDate()) zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    else zdt.format(DateTimeFormatter.ofPattern("MMM d"))
}.getOrDefault("")