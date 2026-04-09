package com.conekt.suite.feature.pulse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.conekt.suite.data.model.*
import com.conekt.suite.feature.chat.ChatListScreen
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableIntStateOf

private enum class PulseTab(val label: String) {
    HOME("Home"), CHATS("Chats"), FEED("Feed"), STORIES("Stories")
}

@Composable
fun PulseScreen(
    onCreatePostClick: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenThread: (convId: String, otherId: String, name: String, avatar: String) -> Unit = { _, _, _, _ -> },
    viewModel: PulseViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(PulseTab.HOME) }

    // Story viewer state — null means viewer is closed
    var viewerStories by remember { mutableStateOf<List<StoryWithAuthor>>(emptyList()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        if (selectedTab == PulseTab.CHATS) {
            // Measure header height: statusBarPadding + topBar(~52dp) + spacer(10) + tabStrip(~44dp) + spacer = ~120dp+statusBar
            ChatListScreen(
                onOpenThread = onOpenThread,
                onOpenProfile = onOpenUserProfile,
                contentTopPadding = 148.dp
            )
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)) {
                PulseTopBar(onCreatePost = onCreatePostClick)
                Spacer(Modifier.height(10.dp))
                PulseTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
            }
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 150.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            when (selectedTab) {
                PulseTab.HOME -> homeItems(
                    state          = state,
                    vm             = viewModel,
                    onProfile      = onOpenUserProfile,
                    onCreatePost   = onCreatePostClick,
                    onOpenStory    = { stories, index ->
                        viewerStories    = stories
                        viewerStartIndex = index
                    }
                )
                PulseTab.FEED -> feedItems(state, onOpenUserProfile, viewModel)
                PulseTab.STORIES -> storiesItems(state, onOpenUserProfile)
                PulseTab.CHATS -> {}
            }
        }

        // Top gradient scrim
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 6.dp)) {
            PulseTopBar(onCreatePost = onCreatePostClick)
            Spacer(Modifier.height(10.dp))
            PulseTabStrip(selected = selectedTab, onSelect = { selectedTab = it })
        }
        // ── Story viewer overlay ─────────────────────────────────────────────
        // Shown on top of everything when a story ring is tapped.
        if (viewerStories.isNotEmpty()) {
            StoryViewerScreen(
                stories    = viewerStories,
                startIndex = viewerStartIndex,
                onClose    = { viewerStories = emptyList() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header + Tab strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseTopBar(onCreatePost: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PulseTab.entries.forEach { tab ->
                val sel = tab == selected
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(
                            if (sel) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)))
                        )
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
// HOME TAB — Premium Dashboard
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.homeItems(
    state: PulseUiState,
    vm: PulseViewModel,
    onProfile: (String) -> Unit,
    onCreatePost: () -> Unit,
    onOpenStory: (stories: List<StoryWithAuthor>, index: Int) -> Unit   // ← new
) {
    // 1. Greeting Hero
    item(key = "greeting") {
        HomeGreetingHero(onCreatePost = onCreatePost)
        Spacer(Modifier.height(18.dp))
    }

    // 2. Active Followers (from stories data = recently active)
    if (state.stories.isNotEmpty()) {
        item(key = "active_followers") {
            HomeActiveFollowers(
                stories    = state.stories,
                onOpenStory = onOpenStory          // ← was: onTap = onProfile
            )
            Spacer(Modifier.height(18.dp))
        }
    }

    // 3. Quick Hub Cards — Music / Notes / Vault
    item(key = "hub_cards") {
        HomeHubCards(
            notesCount = state.recentNotes.size,
            filesCount = state.recentFiles.size
        )
        Spacer(Modifier.height(18.dp))
    }

    // 4. Recent Notes Glimpse
    if (state.recentNotes.isNotEmpty()) {
        item(key = "notes_header") {
            HomeSectionHeader(
                title = "Your Notes",
                subtitle = "${state.recentNotes.size} recent"
            )
            Spacer(Modifier.height(12.dp))
        }
        item(key = "notes_row") {
            HomeNotesRow(notes = state.recentNotes)
            Spacer(Modifier.height(18.dp))
        }
    }

    // 5. Photo Gallery from post media
    val mediaPosts = state.posts.filter { it.mediaUrls.any { u -> u.isNotBlank() } }
    if (mediaPosts.isNotEmpty()) {
        item(key = "gallery_header") {
            HomeSectionHeader(title = "Gallery", subtitle = "from your feed")
            Spacer(Modifier.height(12.dp))
        }
        item(key = "gallery") {
            HomeGalleryMosaic(posts = mediaPosts, onTap = onProfile)
            Spacer(Modifier.height(18.dp))
        }
    }

    // 6. Followers Activity — compact post list
    if (state.posts.isNotEmpty()) {
        item(key = "activity_header") {
            HomeSectionHeader(title = "Followers Activity", subtitle = "latest from your circle")
            Spacer(Modifier.height(12.dp))
        }
        items(state.posts.take(5), key = { "act_${it.id}" }) { post ->
            HomeActivityItem(post = post, onProfile = onProfile, onLike = { vm.likePost(it) })
            Spacer(Modifier.height(10.dp))
        }
    }

    if (state.isLoading) {
        item(key = "loading") {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun HomeGreetingHero(onCreatePost: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val emoji = when {
        hour < 12 -> "☀️"
        hour < 17 -> "⚡"
        else -> "🌙"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A0A05),
                            Color(0xFF200D08),
                            Color(0xFF160810)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(32.dp))
        ) {
            // Ambient glow
            Box(
                Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(BrandEnd.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
            Box(
                Modifier.size(120.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(BrandStart.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )

            Column(Modifier.padding(24.dp)) {
                Text(
                    "$greeting $emoji",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "What's on\nyour mind today?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Compose button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ConektGradient.brandHorizontal)
                            .clickable { onCreatePost() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Create Post", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    // Story button
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .clickable { onCreatePost() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                            Text("Story", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.70f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActiveFollowers(
    stories: List<StoryWithAuthor>,
    onOpenStory: (stories: List<StoryWithAuthor>, index: Int) -> Unit
) {
    // Group stories by author so each ring represents one person.
    val grouped = stories.groupBy { it.author.id }
    val authors = grouped.values.toList()   // list of per-author story lists

    Column {
        HomeSectionHeader(
            title    = "Active Now",
            subtitle = "${authors.size} people"
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(authors, key = { _, list -> list.first().author.id }) { authorIdx, authorStories ->
                val first = authorStories.first()
                val name  = first.author.displayName ?: first.author.username

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(64.dp)
                        // Tap → open story viewer starting at this author's first story.
                        // We flatten all stories into a single list, but start at the
                        // index of this author's first story so the progress bars work.
                        .clickable {
                            val flatIndex = stories.indexOf(first).coerceAtLeast(0)
                            onOpenStory(stories, flatIndex)
                        }
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier
                                .size(58.dp)
                                .background(
                                    Brush.linearGradient(listOf(BrandStart, BrandEnd)),
                                    CircleShape
                                )
                                .padding(2.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(1.5.dp)
                                    .clip(CircleShape)
                            ) {
                                val avatar = first.author.avatarUrl?.ifBlank { null }
                                if (avatar != null) {
                                    AsyncImage(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Box(
                                        Modifier.fillMaxSize().background(BrandEnd.copy(alpha = 0.22f)),
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
                        // Active dot
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        name.substringBefore(" "),
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurface,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHubCards(notesCount: Int, filesCount: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Music card
        HubCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Headphones,
            title = "Music",
            subtitle = "Library",
            accent = InfoBlue,
            gradient = listOf(Color(0xFF0A1A30), Color(0xFF0D1520))
        )
        // Notes card
        HubCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.EditNote,
            title = "Notes",
            subtitle = "$notesCount recent",
            accent = SuccessGreen,
            gradient = listOf(Color(0xFF051A10), Color(0xFF071510))
        )
        // Vault card
        HubCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Folder,
            title = "Vault",
            subtitle = "$filesCount files",
            accent = SoftOrange,
            gradient = listOf(Color(0xFF1A1005), Color(0xFF150D05))
        )
    }
}

@Composable
private fun HubCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    gradient: List<Color>
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(gradient))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f))
            }
        }
    }
}

@Composable
private fun HomeNotesRow(notes: List<NotePreview>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes.take(4), key = { it.id }) { note ->
            val accent = try { Color(android.graphics.Color.parseColor(note.coverColor ?: "#FF7F2E")) } catch (_: Exception) { BrandEnd }
            Box(
                modifier = Modifier.width(200.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.14f), MaterialTheme.colorScheme.surface))
                    )
                    .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                        if (note.isPinned) {
                            Icon(Icons.Rounded.PushPin, null, tint = Color(0xFFFFD86B), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note.body.isNotBlank()) {
                        Text(note.body.take(60), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(relativeTime(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.70f))
                }
            }
        }
    }
}

@Composable
private fun HomeGalleryMosaic(posts: List<PostWithAuthor>, onTap: (String) -> Unit) {
    val allImages = posts.flatMap { post -> post.mediaUrls.filter { it.isNotBlank() }.map { url -> Pair(url, post.author.id) } }.take(9)
    if (allImages.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // First row: 1 large image
        val firstImage = allImages[0]
        Box(
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp))
                .clickable { onTap(firstImage.second) }
        ) {
            AsyncImage(firstImage.first, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)))))
        }

        if (allImages.size > 1) {
            Spacer(Modifier.height(6.dp))
            // Second row: 3 smaller images
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allImages.drop(1).take(3).forEach { (url, authorId) ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                            .clickable { onTap(authorId) }
                    ) {
                        AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                // Fill empty slots if fewer than 3
                repeat(maxOf(0, 3 - (allImages.size - 1))) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (allImages.size > 4) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allImages.drop(4).take(3).forEachIndexed { i, (url, authorId) ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                            .clickable { onTap(authorId) }
                    ) {
                        AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // "X more" overlay on last visible
                        if (i == 2 && allImages.size > 7) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                Text("+${allImages.size - 7}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                repeat(maxOf(0, 3 - minOf(3, allImages.size - 4))) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HomeActivityItem(post: PostWithAuthor, onProfile: (String) -> Unit, onLike: (String) -> Unit) {
    var liked by remember { mutableStateOf(false) }
    val name = post.author.displayName ?: post.author.username

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp)).clickable { onProfile(post.author.id) },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar
            Box(Modifier.size(44.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f)).clickable { onProfile(post.author.id) }) {
                post.author.avatarUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(name.first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                post.body?.ifBlank { null }?.let {
                    Text(it.take(80), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
            }
            // Media thumbnail if present
            val thumb = post.mediaUrls.firstOrNull { it.isNotBlank() }
            if (thumb != null) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            } else {
                // Like action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(if (liked) Color(0xFFFF3B5C).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { liked = !liked; if (liked) onLike(post.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
                            tint = if (liked) Color(0xFFFF3B5C) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                    Text("${post.likeCount + if (liked) 1 else 0}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("See all", style = MaterialTheme.typography.labelMedium, color = BrandEnd)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEED TAB — Editorial Magazine Cards
// ─────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.feedItems(state: PulseUiState, onProfile: (String) -> Unit, vm: PulseViewModel) {
    if (state.isLoading) {
        item(key = "feed_loading") {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        return
    }
    if (state.posts.isEmpty()) {
        item(key = "feed_empty") { FeedEmptyState() }
        return
    }
    items(state.posts, key = { "feed_${it.id}" }) { post ->
        MagazinePostCard(post = post, onProfile = onProfile, onLike = { vm.likePost(it) }, onRepost = { vm.repostPost(it) })
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun MagazinePostCard(
    post: PostWithAuthor,
    onProfile: (String) -> Unit,
    onLike: (String) -> Unit,
    onRepost: (String) -> Unit
) {
    var liked by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val likes = post.likeCount + if (liked) 1 else 0
    val name = post.author.displayName ?: post.author.username
    val validImages = post.mediaUrls.filter { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column {
                if (validImages.isNotEmpty()) {
                    // ── Image area (carousel if multiple) ─────────────────────
                    Box {
                        if (validImages.size == 1) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1.1f)) {
                                AsyncImage(validImages[0], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                // Dark gradient overlay
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
                                // Author chip (overlaid top-left)
                                AuthorChipOverlay(name = name, avatarUrl = post.author.avatarUrl, time = relativeTime(post.createdAt), onClick = { onProfile(post.author.id) })
                                // Action bar (overlaid bottom)
                                ImageActionBar(liked = liked, saved = saved, likeCount = likes, commentCount = post.commentCount, shareCount = post.shareCount,
                                    onLike = { liked = !liked; if (liked) onLike(post.id) },
                                    onComment = {},
                                    onShare = { onRepost(post.id) },
                                    onSave = { saved = !saved }
                                )
                            }
                        } else {
                            // Multi-image carousel
                            val pagerState = rememberPagerState { validImages.size }
                            Box(Modifier.fillMaxWidth().aspectRatio(1.1f)) {
                                HorizontalPager(state = pagerState) { page ->
                                    AsyncImage(validImages[page], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                // Gradient overlays
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
                                // Author chip
                                AuthorChipOverlay(name = name, avatarUrl = post.author.avatarUrl, time = relativeTime(post.createdAt), onClick = { onProfile(post.author.id) })
                                // Page dots (top-right)
                                Row(
                                    Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(validImages.size) { i ->
                                        Box(
                                            Modifier.size(if (i == pagerState.currentPage) 18.dp else 6.dp, 6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.40f))
                                        )
                                    }
                                }
                                // Action bar
                                ImageActionBar(liked = liked, saved = saved, likeCount = likes, commentCount = post.commentCount, shareCount = post.shareCount,
                                    onLike = { liked = !liked; if (liked) onLike(post.id) },
                                    onComment = {}, onShare = { onRepost(post.id) }, onSave = { saved = !saved }
                                )
                            }
                        }
                    }

                    // Caption below image
                    if (!post.body.isNullOrBlank()) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(5.dp))
                            Text(post.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }

                } else {
                    // ── Text-only post ────────────────────────────────────────
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.10f), BrandEnd.copy(alpha = 0.05f), MaterialTheme.colorScheme.surface)))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            // Author row
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f)).clickable { onProfile(post.author.id) }) {
                                    post.author.avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(name.first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                                    Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(post.body ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
                            Spacer(Modifier.height(14.dp))
                            // Inline action row for text posts
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextActionPill(icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    label = "$likes", accent = if (liked) Color(0xFFFF3B5C) else MaterialTheme.colorScheme.onSurfaceVariant) {
                                    liked = !liked; if (liked) onLike(post.id)
                                }
                                TextActionPill(icon = Icons.Rounded.ChatBubbleOutline, label = "${post.commentCount}", accent = MaterialTheme.colorScheme.onSurfaceVariant) {}
                                TextActionPill(icon = Icons.Rounded.Repeat, label = "${post.shareCount}", accent = MaterialTheme.colorScheme.onSurfaceVariant) { onRepost(post.id) }
                                Spacer(Modifier.weight(1f))
                                TextActionPill(icon = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, label = "", accent = if (saved) BrandEnd else MaterialTheme.colorScheme.onSurfaceVariant) { saved = !saved }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.AuthorChipOverlay(name: String, avatarUrl: String?, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f))) {
            avatarUrl?.ifBlank { null }?.let {
                AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(name.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
        Column {
            Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            Text(time, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun BoxScope.ImageActionBar(
    liked: Boolean, saved: Boolean, likeCount: Int, commentCount: Int, shareCount: Int,
    onLike: () -> Unit, onComment: () -> Unit, onShare: () -> Unit, onSave: () -> Unit
) {
    Row(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Like
            ImageActionBtn(icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (likeCount > 0) "$likeCount" else null,
                tint = if (liked) Color(0xFFFF3B5C) else Color.White, onClick = onLike)
            // Comment
            ImageActionBtn(icon = Icons.Rounded.ChatBubbleOutline,
                label = if (commentCount > 0) "$commentCount" else null,
                tint = Color.White, onClick = onComment)
            // Share
            ImageActionBtn(icon = Icons.Rounded.Repeat,
                label = if (shareCount > 0) "$shareCount" else null,
                tint = Color.White, onClick = onShare)
        }
        Spacer(Modifier.weight(1f))
        // Save
        ImageActionBtn(icon = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, label = null, tint = if (saved) BrandEnd else Color.White, onClick = onSave)
    }
}

@Composable
private fun ImageActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String?, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        label?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun TextActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
    }
}

@Composable
private fun FeedEmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(26.dp)).background(ConektGradient.brandHorizontal),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Follow people and post to see activity here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STORIES TAB
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

@Composable
private fun StoryFullCard(story: StoryWithAuthor, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun relativeTime(iso: String): String = try {
    val d = java.time.Duration.between(Instant.parse(iso), Instant.now())
    when {
        d.toMinutes() < 1 -> "just now"
        d.toHours() < 1 -> "${d.toMinutes()}m"
        d.toDays() < 1 -> "${d.toHours()}h"
        d.toDays() < 7 -> "${d.toDays()}d"
        else -> Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d"))
    }
} catch (_: Exception) { "" }