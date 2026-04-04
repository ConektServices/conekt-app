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
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class PulseTab { HOME, COMMUNITY, FEED, STORIES }

@Composable
fun PulseScreen(
    onCreatePostClick: () -> Unit = {},
    onOpenChat:        () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: PulseViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(PulseTab.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Scrollable content ────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(top = 148.dp, bottom = 185.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (selectedTab) {
                PulseTab.HOME      -> homeItems(state, onOpenUserProfile)
                PulseTab.COMMUNITY -> communityItems(state, onOpenUserProfile)
                PulseTab.FEED      -> feedItems(state, onOpenUserProfile)
                PulseTab.STORIES   -> storiesItems(state, onOpenUserProfile)
            }
        }

        // ── Top gradient scrim ────────────────────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(270.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.50f), Color.Transparent)))
        )

        // ── Header + tabs ─────────────────────────────────────────────────────
        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)) {
            PulseHeader(
                onCreatePost = onCreatePostClick,
                onOpenChat   = onOpenChat
            )
            Spacer(Modifier.height(10.dp))
            PulseTabRow(selectedTab) { selectedTab = it }
        }
    }
}

// ── HOME tab ──────────────────────────────────────────────────────────────────

private fun LazyListScope.homeItems(state: PulseUiState, onProfile: (String) -> Unit) {
    // Stories strip
    if (state.stories.isNotEmpty()) {
        item(key = "stories_strip") {
            StoriesStrip(state.stories) { onProfile(it) }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (state.isLoading) {
        item(key = "loading") {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    if (state.posts.isEmpty() && state.stories.isEmpty()) {
        item(key = "empty_home") {
            EmptyFeedCard()
        }
        return
    }

    // Feed posts
    items(state.posts.take(6), key = { "home_${it.id}" }) { post ->
        FeedPostCard(post, onProfile)
        Spacer(Modifier.height(1.dp))
    }
}

// ── COMMUNITY tab ─────────────────────────────────────────────────────────────

private fun LazyListScope.communityItems(state: PulseUiState, onProfile: (String) -> Unit) {
    item(key = "community_header") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Discover people and posts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.isLoading) {
        item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) } }
    } else if (state.posts.isEmpty()) {
        item(key = "community_empty") { EmptyFeedCard() }
    } else {
        items(state.posts, key = { "com_${it.id}" }) { post ->
            CommunityRow(post, onProfile)
        }
    }
}

// ── FEED tab ──────────────────────────────────────────────────────────────────

private fun LazyListScope.feedItems(state: PulseUiState, onProfile: (String) -> Unit) {
    if (state.posts.isEmpty()) {
        item(key = "feed_empty") {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.DynamicFeed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("Your feed is empty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Follow people to see their posts here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        items(state.posts, key = { "feed_${it.id}" }) { post ->
            FeedPostCard(post, onProfile)
            Spacer(Modifier.height(1.dp))
        }
    }
}

// ── STORIES tab ───────────────────────────────────────────────────────────────

private fun LazyListScope.storiesItems(state: PulseUiState, onProfile: (String) -> Unit) {
    val storyGroups = state.stories.groupBy { it.author.id }
    if (storyGroups.isEmpty()) {
        item(key = "stories_empty") {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("No stories yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Stories from people you follow appear here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        items(storyGroups.entries.toList(), key = { "sg_${it.key}" }) { (authorId, stories) ->
            val first = stories.first()
            StoryFullCard(story = first, onClick = { onProfile(authorId) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseHeader(onCreatePost: () -> Unit, onOpenChat: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Conekt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("your connected space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Chat / messages button — navigates to Chat screen
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable { onOpenChat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ChatBubble, "Chat", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // New post
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(ConektGradient.brandHorizontal)
                    .clickable { onCreatePost() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, "Create post", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun PulseTabRow(selected: PulseTab, onSelect: (PulseTab) -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape           = RoundedCornerShape(22.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        shadowElevation = 18.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PulseTab.entries.forEach { tab ->
                val sel = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (sel) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)))
                        )
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = tab.name,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color      = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoriesStrip(stories: List<StoryWithAuthor>, onTap: (String) -> Unit) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val unique = stories.distinctBy { it.author.id }
        items(unique.take(12), key = { it.id }) { story ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp).clickable { onTap(story.author.id) }) {
                Box(
                    modifier = Modifier.size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)))
                        .padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(2.dp)) {
                        val avatar = story.author.avatarUrl?.ifBlank { null }
                        if (avatar != null) {
                            AsyncImage(avatar, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(BrandEnd.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                                Text((story.author.displayName ?: story.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(story.author.displayName ?: story.author.username, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feed post card — Instagram-style full-width
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedPostCard(post: PostWithAuthor, onProfile: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        // Author row
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.18f)).clickable { onProfile(post.author.id) }
            ) {
                post.author.avatarUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text((post.author.displayName ?: post.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text(formatRelTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        // Media (first image if present)
        val mediaUrl = post.mediaUrls.firstOrNull()?.ifBlank { null }
        if (mediaUrl != null) {
            AsyncImage(
                model       = mediaUrl,
                contentDescription = null,
                modifier    = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }

        // Actions row
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Rounded.ChatBubbleOutline, "Comment", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Rounded.Send, "Share", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.BookmarkBorder, "Save", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        }

        // Like count + caption
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            if (post.likeCount > 0) {
                Text("${post.likeCount} likes", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
            }
            val caption = post.body?.ifBlank { null }
            if (caption != null) {
                Row {
                    Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(6.dp))
                    Text(caption, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (post.commentCount > 0) {
                Text("View all ${post.commentCount} comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Community row (compact)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommunityRow(post: PostWithAuthor, onProfile: (String) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onProfile(post.author.id) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
            post.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text((post.author.displayName ?: post.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(post.body?.take(80) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        post.mediaUrls.firstOrNull()?.ifBlank { null }?.let { url ->
            AsyncImage(url, null, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Story full card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoryFullCard(story: StoryWithAuthor, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { onClick() },
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.85f)) {
            AsyncImage(story.mediaUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.70f)))))
            Row(
                modifier          = Modifier.align(Alignment.TopStart).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).border(2.dp, BrandEnd, CircleShape)) {
                    story.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Spacer(Modifier.width(8.dp))
                Text(story.author.displayName ?: story.author.username, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            story.caption?.ifBlank { null }?.let { caption ->
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyFeedCard() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = BrandEnd.copy(alpha = 0.40f), modifier = Modifier.size(52.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Follow people and post content to see it here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun formatRelTime(iso: String): String {
    return try {
        val then = Instant.parse(iso)
        val diff = java.time.Duration.between(then, Instant.now())
        when {
            diff.toMinutes() < 1  -> "just now"
            diff.toHours()   < 1  -> "${diff.toMinutes()}m"
            diff.toDays()    < 1  -> "${diff.toHours()}h"
            diff.toDays()    < 7  -> "${diff.toDays()}d"
            else                  -> then.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) { "" }
}