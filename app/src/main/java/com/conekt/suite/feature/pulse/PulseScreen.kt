package com.conekt.suite.feature.pulse

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.PostWithAuthor
import com.conekt.suite.data.model.StoryWithAuthor
import com.conekt.suite.feature.chat.ChatListScreen
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class PulseTab(val label: String) {
    HOME("Home"), CHATS("Chats"), FEED("Feed"), STORIES("Stories")
}

@Composable
fun PulseScreen(
    onCreatePostClick: () -> Unit = {},
    onOpenChat:        () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenThread: (convId: String, otherId: String, name: String, avatar: String) -> Unit = { _, _, _, _ -> },
    viewModel: PulseViewModel = viewModel()
) {
    val state       by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(PulseTab.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── CHATS tab: full-screen embedded ChatListScreen ────────────────────
        if (selectedTab == PulseTab.CHATS) {
            ChatListScreen(
                onOpenThread  = onOpenThread,
                onOpenProfile = onOpenUserProfile
            )
            // Tab strip overlaid at top (with its own padding)
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)) {
                PulseTopBar(onCreatePost = onCreatePostClick)
                Spacer(Modifier.height(10.dp))
                PulseTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
            }
            return@Box
        }

        // ── Scrollable body (HOME / FEED / STORIES) ───────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(top = 150.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (selectedTab) {
                PulseTab.HOME    -> homeItems(state, viewModel, onOpenUserProfile)
                PulseTab.FEED    -> feedItems(state, onOpenUserProfile)
                PulseTab.STORIES -> storiesItems(state, onOpenUserProfile)
                PulseTab.CHATS   -> { /* handled above */ }
            }
        }

        // Top gradient
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(260.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.52f), Color.Transparent)))
        )

        // Header + tabs
        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)) {
            PulseTopBar(onCreatePost = onCreatePostClick)
            Spacer(Modifier.height(10.dp))
            PulseTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header + tab strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseTopBar(onCreatePost: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Conekt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("your connected space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onCreatePost() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, "New post", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun PulseTabStrip(selected: PulseTab, onSelect: (PulseTab) -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape           = RoundedCornerShape(22.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        shadowElevation = 18.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PulseTab.entries.forEach { tab ->
                val sel = tab == selected
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f))))
                        .clickable { onSelect(tab) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME tab items
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.homeItems(state: PulseUiState, vm: PulseViewModel, onProfile: (String) -> Unit) {
    // Stories strip
    item(key = "stories_strip") {
        StoriesStrip(stories = state.stories, onTap = onProfile)
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    }
    when {
        state.isLoading -> item(key = "loading") {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        state.posts.isEmpty() -> item(key = "empty") { EmptyFeedCard() }
        else -> items(state.posts, key = { "home_${it.id}" }) { post ->
            PostCard(post = post, onProfile = onProfile, onLike = { vm.likePost(it) }, onRepost = { vm.repostPost(it) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEED tab items
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.feedItems(state: PulseUiState, onProfile: (String) -> Unit) {
    if (state.posts.isEmpty()) {
        item(key = "feed_empty") {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.DynamicFeed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("Your feed is empty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Follow people to see posts here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        items(state.posts, key = { "feed_${it.id}" }) { post ->
            PostCard(post = post, onProfile = onProfile, onLike = {}, onRepost = {})
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STORIES tab items
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.storiesItems(state: PulseUiState, onProfile: (String) -> Unit) {
    val groups = state.stories.groupBy { it.author.id }
    if (groups.isEmpty()) {
        item(key = "st_empty") {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No stories yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Stories from people you follow appear here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        items(groups.entries.toList(), key = { "sg_${it.key}" }) { (authorId, stories) ->
            StoryFullCard(story = stories.first(), onClick = { onProfile(authorId) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories strip component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoriesStrip(stories: List<StoryWithAuthor>, onTap: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // "Your story" add button
        item(key = "add_story") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                    Box(
                        Modifier.size(22.dp).offset(y = 5.dp).clip(CircleShape).background(BrandEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Your story", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
        items(stories.distinctBy { it.author.id }.take(15), key = { it.id }) { story ->
            val name = story.author.displayName ?: story.author.username
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp).clickable { onTap(story.author.id) }) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))).padding(2.5.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(2.dp)) {
                        val avatar = story.author.avatarUrl?.ifBlank { null }
                        if (avatar != null) {
                            AsyncImage(avatar, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                                Text(name.first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Post card — Instagram style with like / comment / repost / save
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PostCard(
    post:      PostWithAuthor,
    onProfile: (String) -> Unit,
    onLike:    (String) -> Unit,
    onRepost:  (String) -> Unit
) {
    var liked  by remember { mutableStateOf(false) }
    var saved  by remember { mutableStateOf(false) }
    val likes  = post.likeCount + if (liked) 1 else 0

    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {

        // Author row
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(BrandEnd.copy(alpha = 0.16f))
                    .clickable { onProfile(post.author.id) }
            ) {
                post.author.avatarUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        (post.author.displayName ?: post.author.username).first().uppercaseChar().toString(),
                        color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    post.author.displayName ?: post.author.username,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    style      = MaterialTheme.typography.bodyMedium
                )
                Text(pulseFmtTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        // Text-only post body (before media)
        post.body?.ifBlank { null }?.takeIf { post.mediaUrls.isEmpty() }?.let { text ->
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }

        // Media — only show if URL is actually present and non-blank
        // This is the fix for the "null value in media_type" crash:
        // Never pass null/blank URLs to AsyncImage
        val mediaUrl = post.mediaUrls.firstOrNull { it.isNotBlank() }
        if (mediaUrl != null) {
            AsyncImage(
                model        = mediaUrl,
                contentDescription = null,
                modifier     = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }

        // Action row
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { liked = !liked; if (liked) onLike(post.id) }, contentAlignment = Alignment.Center) {
                Icon(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Like",
                    tint     = if (liked) Color(0xFFFF3B5C) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
            // Comment
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChatBubbleOutline, "Comment", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            // Repost
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { onRepost(post.id) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Repeat, "Repost", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            // Share to chat
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Send, "Share", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.weight(1f))
            // Save
            Box(Modifier.size(42.dp).clip(CircleShape).clickable { saved = !saved }, contentAlignment = Alignment.Center) {
                Icon(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Save", tint = if (saved) BrandEnd else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }

        // Like count
        if (likes > 0) {
            Text("$likes ${if (likes == 1) "like" else "likes"}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 1.dp))
        }

        // Caption below media
        post.body?.ifBlank { null }?.takeIf { post.mediaUrls.isNotEmpty() }?.let { cap ->
            Row(Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(5.dp))
                Text(cap, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        // Comment count tap
        if (post.commentCount > 0) {
            Text("View all ${post.commentCount} comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp).clickable { })
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Story full-card (STORIES tab)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoryFullCard(story: StoryWithAuthor, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.85f)) {
            if (story.mediaUrl.isNotBlank()) {
                AsyncImage(story.mediaUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.38f), BrandEnd.copy(alpha = 0.28f)))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.65f)))))
            Row(Modifier.align(Alignment.TopStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, BrandEnd, CircleShape)) {
                    story.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Spacer(Modifier.width(8.dp))
                Text(story.author.displayName ?: story.author.username, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            story.caption?.ifBlank { null }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyFeedCard() {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = BrandEnd.copy(alpha = 0.36f), modifier = Modifier.size(52.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Follow people and post to see activity here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun pulseFmtTime(iso: String): String = runCatching {
    val d = java.time.Duration.between(Instant.parse(iso), Instant.now())
    when {
        d.toMinutes() < 1  -> "just now"
        d.toHours()   < 1  -> "${d.toMinutes()}m"
        d.toDays()    < 1  -> "${d.toHours()}h"
        d.toDays()    < 7  -> "${d.toDays()}d"
        else -> Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d"))
    }
}.getOrDefault("")