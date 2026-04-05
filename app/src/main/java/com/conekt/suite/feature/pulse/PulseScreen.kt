package com.conekt.suite.feature.pulse

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

private enum class PulseTab(val label: String) {
    HOME("Home"), COMMUNITY("Community"), FEED("Feed"), STORIES("Stories")
}

@Composable
fun PulseScreen(
    onCreatePostClick: () -> Unit = {},
    onOpenChat:        () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: PulseViewModel = viewModel()
) {
    val state      by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(PulseTab.HOME) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Scrollable body ───────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(top = 150.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (selectedTab) {
                PulseTab.HOME      -> homeItems(state, viewModel, onOpenUserProfile)
                PulseTab.COMMUNITY -> communityItems(state, onOpenUserProfile)
                PulseTab.FEED      -> feedItems(state, onOpenUserProfile)
                PulseTab.STORIES   -> storiesItems(state, onOpenUserProfile)
            }
        }

        // ── Top scrim ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(265.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )

        // ── Header + tabs ─────────────────────────────────────────────────────
        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)) {
            PulseTopBar(onCreatePost = onCreatePostClick, onOpenChat = onOpenChat)
            Spacer(Modifier.height(10.dp))
            PulseTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseTopBar(onCreatePost: () -> Unit, onOpenChat: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Conekt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("your connected space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // Chat icon — opens messages
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable { onOpenChat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ChatBubble, "Chat", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp))
            }
            // Create post
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
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (sel) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f))))
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME tab
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.homeItems(state: PulseUiState, vm: PulseViewModel, onProfile: (String) -> Unit) {
    // Stories strip
    if (state.stories.isNotEmpty()) {
        item(key = "stories_strip") {
            PulseStoriesStrip(stories = state.stories, onTap = onProfile)
            Spacer(Modifier.height(16.dp))
        }
    }

    when {
        state.isLoading -> {
            item(key = "home_loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
        }
        state.posts.isEmpty() && state.stories.isEmpty() -> {
            item(key = "home_empty") { PulseEmptyCard() }
        }
        else -> {
            items(state.posts.take(8), key = { "hp_${it.id}" }) { post ->
                FeedCard(post, onProfile)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMMUNITY tab
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.communityItems(state: PulseUiState, onProfile: (String) -> Unit) {
    item(key = "com_title") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Discover what people are sharing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.isLoading) {
        item(key = "com_loading") {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
    } else if (state.posts.isEmpty()) {
        item(key = "com_empty") { PulseEmptyCard() }
    } else {
        items(state.posts, key = { "cp_${it.id}" }) { post ->
            CommunityCompactRow(post, onProfile)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEED tab — Instagram-style full posts
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.feedItems(state: PulseUiState, onProfile: (String) -> Unit) {
    if (state.posts.isEmpty()) {
        item(key = "feed_empty") {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.DynamicFeed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Text("Your feed is empty", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Follow people to see their posts here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        items(state.posts, key = { "fp_${it.id}" }) { post ->
            FeedCard(post, onProfile)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STORIES tab
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.storiesItems(state: PulseUiState, onProfile: (String) -> Unit) {
    val grouped = state.stories.groupBy { it.author.id }
    if (grouped.isEmpty()) {
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
        items(grouped.entries.toList(), key = { "sg_${it.key}" }) { (authorId, stories) ->
            StoryFullCard(story = stories.first(), onClick = { onProfile(authorId) })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseStoriesStrip(stories: List<StoryWithAuthor>, onTap: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(stories.distinctBy { it.author.id }.take(14), key = { it.id }) { story ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp).clickable { onTap(story.author.id) }) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))).padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(2.dp)) {
                        val avatar = story.author.avatarUrl?.ifBlank { null }
                        if (avatar != null) {
                            AsyncImage(avatar, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
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

@Composable
private fun FeedCard(post: PostWithAuthor, onProfile: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        // Author header
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f)).clickable { onProfile(post.author.id) }) {
                post.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((post.author.displayName ?: post.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text(pulseFmtTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        // Media
        post.mediaUrls.firstOrNull()?.ifBlank { null }?.let { url ->
            AsyncImage(url, null, modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
        }

        // Actions
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FavoriteBorder, "Like",    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Rounded.ChatBubbleOutline, "Comment", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Rounded.Send,    "Share",  tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.BookmarkBorder, "Save", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        }

        // Caption
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            if (post.likeCount > 0) {
                Text("${post.likeCount} likes", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
            }
            post.body?.ifBlank { null }?.let { caption ->
                Row {
                    Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(5.dp))
                    Text(caption, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (post.commentCount > 0) {
                Text("View all ${post.commentCount} comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CommunityCompactRow(post: PostWithAuthor, onProfile: (String) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onProfile(post.author.id) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.14f))) {
            post.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text((post.author.displayName ?: post.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(post.author.displayName ?: post.author.username, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            post.body?.ifBlank { null }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        post.mediaUrls.firstOrNull()?.ifBlank { null }?.let { url ->
            AsyncImage(url, null, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun StoryFullCard(story: StoryWithAuthor, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { onClick() },
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.85f)) {
            AsyncImage(story.mediaUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.65f)))))
            Row(
                modifier          = Modifier.align(Alignment.TopStart).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, BrandEnd, CircleShape)) {
                    story.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Spacer(Modifier.width(8.dp))
                Text(story.author.displayName ?: story.author.username, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
            story.caption?.ifBlank { null }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
        }
    }
}

@Composable
private fun PulseEmptyCard() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 52.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = BrandEnd.copy(alpha = 0.38f), modifier = Modifier.size(52.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Follow people and post to see activity here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun pulseFmtTime(iso: String): String = runCatching {
    val then = Instant.parse(iso)
    val diff = java.time.Duration.between(then, Instant.now())
    when {
        diff.toMinutes() < 1  -> "just now"
        diff.toHours()   < 1  -> "${diff.toMinutes()}m"
        diff.toDays()    < 1  -> "${diff.toHours()}h"
        diff.toDays()    < 7  -> "${diff.toDays()}d"
        else -> then.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d"))
    }
}.getOrDefault("")