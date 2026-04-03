package com.conekt.suite.feature.pulse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.PostWithAuthor
import com.conekt.suite.data.model.StoryWithAuthor
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

// ── UI model types ────────────────────────────────────────────────────────────

data class StoryUi(val name: String, val imageUrl: String, val isOnline: Boolean)

data class PostUi(
    val author: String, val handle: String,
    val imageUrl: String, val caption: String, val stats: String
)

data class TrackUi(val title: String, val artist: String, val coverUrl: String, val duration: String)

data class PersonalNoteUi(val title: String, val body: String, val mood: String, val updatedAt: String)

data class PostedNoteUi(
    val author: String, val handle: String, val avatarUrl: String,
    val title: String, val body: String, val tag: String, val stats: String
)

private enum class PulseTopTab { HOME, FEED, STORIES }

private data class LiveCardUi(
    val title: String, val creator: String, val followers: String,
    val imageUrl: String, val stats: String
)

private data class NotePeekUi(val title: String, val body: String, val author: String, val tag: String)

data class StorySceneUi(
    val author: String, val handle: String,
    val backgroundUrl: String, val previewUrl: String, val caption: String,
    val reactions: List<StoryReactionUi>, val tags: List<String>,
    val views: String, val location: String
)

data class StoryReactionUi(
    val avatarUrl: String, val userHandle: String, val text: String, val emoji: String
)

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun PostWithAuthor.toUi() = PostUi(
    author   = author.displayName ?: author.username,
    handle   = "@${author.username}",
    imageUrl = mediaUrls.firstOrNull().orEmpty(),
    caption  = body.orEmpty(),
    stats    = "$likeCount likes • $commentCount comments"
)

private fun StoryWithAuthor.toCircleUi() = StoryUi(
    name     = author.displayName ?: author.username,
    imageUrl = author.avatarUrl.orEmpty().ifBlank { mediaUrl },
    isOnline = true
)

private fun StoryWithAuthor.toSceneUi() = StorySceneUi(
    author        = author.displayName ?: author.username,
    handle        = "@${author.username}",
    backgroundUrl = mediaUrl,
    previewUrl    = mediaUrl,
    caption       = caption.orEmpty(),
    reactions     = emptyList(),
    tags          = emptyList(),
    views         = "recent",
    location      = ""
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PulseScreen(
    onCreatePostClick: () -> Unit = {},
    viewModel: PulseViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var selectedTab  by rememberSaveable { mutableStateOf(PulseTopTab.HOME) }
    var currentTrack by remember { mutableStateOf<TrackUi?>(null) }
    var isPlaying    by remember { mutableStateOf(true) }

    // ── Real data ──────────────────────────────────────────────────────────────
    val circleItems  = state.stories.distinctBy { it.author.id }.map { it.toCircleUi() }
    val realPosts    = state.posts.map { it.toUi() }
    val realScenes   = state.stories.map { it.toSceneUi() }

    // ── Demo fallback data ────────────────────────────────────────────────────
    val demoStories = listOf(
        StoryUi("Lina",  "https://images.unsplash.com/photo-1438761681033-6461ffad8d80", true),
        StoryUi("Amos",  "https://images.unsplash.com/photo-1500648767791-00dcc994a43e", false),
        StoryUi("Maya",  "https://images.unsplash.com/photo-1544005313-94ddf0286df2", true),
        StoryUi("Noah",  "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d", false),
        StoryUi("Nia",   "https://images.unsplash.com/photo-1494790108377-be9c29b29330", true)
    )

    val tracks = listOf(
        TrackUi("Midnight Echo", "Arielle",
            "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f", "3:42"),
        TrackUi("Glowline", "Miles",
            "https://images.unsplash.com/photo-1511379938547-c1f69419868d", "2:58"),
        TrackUi("Soft Orbit", "Nia",
            "https://images.unsplash.com/photo-1494232410401-ad00d5433cfa", "4:10")
    )

    val personalNotes = listOf(
        PersonalNoteUi("Tonight ideas",    "A softer feed, less noise, more meaning.", "Focus", "Edited 12m ago"),
        PersonalNoteUi("Upload reminder",  "Turn design drafts into shareable stories.", "Work",  "Edited 48m ago"),
        PersonalNoteUi("Music thought",    "Let listening activity feel alive but calm.", "Mood", "Edited 1h ago")
    )

    val demoPosts = listOf(
        PostedNoteUi("Elena Juni", "@elena.juni",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
            "Digital spaces should feel alive",
            "I think the best platforms in the next wave will not separate notes, files, and social interaction too hard.",
            "thought", "82 reactions • 21 comments"),
        PostedNoteUi("Daniel Moss", "@daniel.moss",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e",
            "Notes can be posts too",
            "A note does not have to be hidden in a private editor forever.",
            "note post", "64 reactions • 16 comments")
    )

    val demoVisualPosts = listOf(
        PostUi("Arielle", "@arielle",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9",
            "Shared a fresh moodboard to Conekt. Gallery sync feels smooth already.",
            "1.2k likes • 148 comments"),
        PostUi("Miles", "@miles",
            "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee",
            "Uploaded a travel set and turned the folder into a story collection.",
            "824 likes • 61 comments")
    )

    val demoScenes = listOf(
        StorySceneUi(
            author = "Rose Oak", handle = "@rose_oak",
            backgroundUrl = "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=80",
            previewUrl    = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80",
            caption = "Just cooking morning breakfast with @uix.vikram @olive_bennet",
            reactions = listOf(
                StoryReactionUi("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80", "@karina_012", "send reaction", "❤️"),
                StoryReactionUi("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80", "@amanda_loo", "send reaction", "😍")
            ),
            tags = listOf("#morning", "#breakfast", "#pumpkin_pie"),
            views = "346k views", location = "Munnar, Kerala"
        )
    )

    // Use real data if available, otherwise fall back to demo
    val circleToShow   = if (circleItems.isNotEmpty()) circleItems else null // null = hide
    val feedPosts      = if (realPosts.isNotEmpty()) realPosts else demoVisualPosts
    val scenesToShow   = if (realScenes.isNotEmpty()) realScenes else demoScenes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 148.dp, bottom = 185.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                PulseTopTab.HOME -> {
                    // Circle — only shown if real friends/story authors exist
                    if (circleToShow != null) {
                        item { PresenceProfilesSection(circleToShow) }
                    }
                    item { LiveCardsSection() }
                    item { QuickActionsSection() }
                    item { HorizontalNotesSection(personalNotes) }
                    item {
                        MusicSection(
                            tracks       = tracks,
                            onTrackClick = { currentTrack = it; isPlaying = true }
                        )
                    }
                    item { StorageInsightCard() }
                }

                PulseTopTab.FEED -> {
                    item { FeedIntroCard() }
                    item { PersonalNotesRail(personalNotes) }

                    if (realPosts.isNotEmpty()) {
                        // Real visual posts at top
                        item {
                            Column {
                                SectionTitle("Recent posts")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(realPosts) { post ->
                            FeedVisualCard(post)
                        }
                    }

                    // Demo note posts always shown (until notes system is live)
                    item {
                        Column {
                            SectionTitle("Shared notes")
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    items(demoPosts) { note ->
                        PostedNoteCard(note)
                    }

                    if (realPosts.isEmpty()) {
                        item {
                            Column {
                                SectionTitle("Visual posts")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(demoVisualPosts) { post ->
                            FeedVisualCard(post)
                        }
                    }
                }

                PulseTopTab.STORIES -> {
                    item {
                        Column {
                            SectionTitle(
                                if (realScenes.isNotEmpty()) "Stories" else "Stories"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    items(scenesToShow) { story ->
                        StorySceneCard(story)
                    }
                }
            }
        }

        // Top gradient
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            PulseHeader(onCreatePostClick = onCreatePostClick)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier         = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PulseTopTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            }
        }

        AnimatedVisibility(
            visible  = currentTrack != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 84.dp)
        ) {
            currentTrack?.let { track ->
                FloatingMusicBar(
                    track       = track,
                    isPlaying   = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    onClose     = { currentTrack = null }
                )
            }
        }
    }
}

// ── Header — no menu button, + button added ───────────────────────────────────

@Composable
private fun PulseHeader(onCreatePostClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Conekt",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = "your connected space",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notifications
            Surface(
                modifier        = Modifier.size(38.dp),
                shape           = CircleShape,
                color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                tonalElevation  = 0.dp,
                shadowElevation = 14.dp,
                border          = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.10f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Rounded.NotificationsNone,
                        contentDescription = "Notifications",
                        tint               = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Create post / story
            Surface(
                modifier        = Modifier.size(38.dp),
                shape           = CircleShape,
                color           = Color.Transparent,
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(ConektGradient.brandHorizontal)
                        .clickable { onCreatePostClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Add,
                        contentDescription = "Create post",
                        tint               = Color.White,
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseTopTabs(selectedTab: PulseTopTab, onTabSelected: (PulseTopTab) -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape           = RoundedCornerShape(22.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        tonalElevation  = 0.dp,
        shadowElevation = 18.dp,
        border          = androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PulseTopTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = when (tab) {
                            PulseTopTab.HOME    -> "HOME"
                            PulseTopTab.FEED    -> "FEED"
                            PulseTopTab.STORIES -> "STORIES"
                        },
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (selected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Circle section ────────────────────────────────────────────────────────────

@Composable
private fun PresenceProfilesSection(stories: List<StoryUi>) {
    Column {
        Text(
            text       = "Your circle",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stories) { story -> SmallPresenceAvatar(story) }
        }
    }
}

@Composable
private fun SmallPresenceAvatar(story: StoryUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ConektGradient.brandHorizontal, CircleShape)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
            ) {
                if (story.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model              = story.imageUrl,
                        contentDescription = story.name,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ConektGradient.brandHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = story.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (story.isOnline) Color(0xFF2BD96B) else Color(0xFFFF4D5B))
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
        Text(
            text     = story.name,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ── Live cards (demo) ─────────────────────────────────────────────────────────

@Composable
private fun LiveCardsSection() {
    val cards = listOf(
        LiveCardUi("Winds of Destiny", "Marina", "119 followers",
            "https://images.unsplash.com/photo-1516321318423-f06f85e504b3", "2m • 86.5k"),
        LiveCardUi("Soft Frequency",   "Nia",    "88 followers",
            "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f", "1m • 42.1k")
    )

    Column {
        Text(
            text       = "Live spaces",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(cards) { card -> LiveShowcaseCard(card) }
        }
    }
}

@Composable
private fun LiveShowcaseCard(card: LiveCardUi) {
    Card(
        modifier  = Modifier.size(width = 205.dp, height = 263.dp),
        shape     = RoundedCornerShape(30.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.imageUrl, contentDescription = card.title,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.82f)))
            ))
            Surface(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = 0.38f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = card.creator, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = card.followers, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(14.dp), color = Color(0xFFFFC34D).copy(alpha = 0.92f)
            ) {
                Text(text = "Follow", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(text = card.title, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(10.dp), color = BrandEnd) {
                        Text(text = "LIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = card.stats, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Quick space (demo) ────────────────────────────────────────────────────────

@Composable
private fun QuickActionsSection() {
    Column {
        SectionTitle("Quick space")
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(Modifier.weight(1f), "Gallery",  "1,284 items",   Icons.Rounded.Collections, listOf(BrandStart, BrandEnd))
            QuickActionCard(Modifier.weight(1f), "Music",    "Now active",    Icons.Rounded.Headphones,  listOf(InfoBlue, BrandEnd))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(Modifier.weight(1f), "Docs",     "34 recent",     Icons.Rounded.Description, listOf(SuccessGreen, InfoBlue))
            QuickActionCard(Modifier.weight(1f), "People",   "12 online",     Icons.Rounded.PeopleAlt,   listOf(BrandEnd, Color(0xFF9B4DFF)))
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier, title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, gradient: List<Color>
) {
    Card(
        modifier  = modifier, shape = RoundedCornerShape(26.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.background(Brush.linearGradient(colors = listOf(gradient.first().copy(alpha = 0.22f), gradient.last().copy(alpha = 0.04f)))).padding(18.dp)) {
            Column {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(gradient)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color.White)
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ── Notes (demo) ──────────────────────────────────────────────────────────────

@Composable
private fun HorizontalNotesSection(notes: List<PersonalNoteUi>) {
    val notePeeks = listOf(
        NotePeekUi("Design thoughts", "Conekt should feel like a digital atmosphere.", "Elena Juni", "idea"),
        NotePeekUi("Shared note", "Notes in HOME should preview beautifully, then open in FEED.", "Daniel Moss", "product"),
        NotePeekUi("Mood direction", "Glass, warmth, depth, and connected spaces.", "Arielle", "ui")
    )
    Column {
        Text(text = "Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(notePeeks) { note -> HomeNoteCard(note) }
        }
    }
}

@Composable
private fun HomeNoteCard(note: NotePeekUi) {
    Card(
        modifier  = Modifier.size(width = 270.dp, height = 165.dp),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.12f), BrandEnd.copy(alpha = 0.05f), MaterialTheme.colorScheme.surface))).padding(18.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)) {
                    Text(text = note.tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(text = note.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 14.dp))
                Text(text = note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(text = note.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ── Music (demo) ──────────────────────────────────────────────────────────────

@Composable
private fun MusicSection(tracks: List<TrackUi>, onTrackClick: (TrackUi) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Music")
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Recent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                tracks.forEachIndexed { index, track ->
                    MusicTrackRow(track = track, onClick = { onTrackClick(track) })
                    if (index != tracks.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MusicTrackRow(track: TrackUi, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onClick() }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = track.coverUrl, contentDescription = track.title, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = track.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = track.duration, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FloatingMusicBar(track: TrackUi, isPlaying: Boolean, onPlayPause: () -> Unit, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 10.dp, shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = track.coverUrl, contentDescription = track.title, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = track.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPlayPause) {
                Icon(imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Storage insight (demo) ────────────────────────────────────────────────────

@Composable
private fun StorageInsightCard() {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = "Your space", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "2.4 GB of 5 GB used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(0.48f).height(10.dp).background(ConektGradient.brandHorizontal))
            }
            Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightChip("Images 41%")
                InsightChip("Audio 24%")
                InsightChip("Docs 13%")
            }
        }
    }
}

// ── Feed ──────────────────────────────────────────────────────────────────────

@Composable
private fun FeedIntroCard() {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.14f), BrandEnd.copy(alpha = 0.08f), MaterialTheme.colorScheme.surface))).padding(20.dp)) {
            Column {
                Text(text = "Feed, rethought", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Not just posts. Thoughts, personal notes, visuals, and shared moments flowing together.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightChip("notes"); InsightChip("visuals"); InsightChip("meaningful")
                }
            }
        }
    }
}

@Composable
private fun PersonalNotesRail(notes: List<PersonalNoteUi>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Your notes")
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Canvas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(notes) { note -> PersonalNoteMiniCard(note) }
        }
    }
}

@Composable
private fun PersonalNoteMiniCard(note: PersonalNoteUi) {
    Card(
        modifier  = Modifier.size(width = 245.dp, height = 170.dp),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.12f), BrandEnd.copy(alpha = 0.04f), MaterialTheme.colorScheme.surface))).padding(18.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
                    Text(text = note.mood, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(text = note.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 14.dp))
                Text(text = note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(text = note.updatedAt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PostedNoteCard(note: PostedNoteUi) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = note.avatarUrl, contentDescription = note.author, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(text = note.author, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = note.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                    Text(text = note.tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(text = note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 16.dp))
            Text(text = note.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Text(text = note.stats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
            Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill(Icons.Rounded.FavoriteBorder, "Like")
                ActionPill(Icons.Rounded.Send, "Share")
                ActionPill(Icons.Rounded.NorthEast, "Open")
            }
        }
    }
}

@Composable
private fun FeedVisualCard(post: PostUi) {
    Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            if (post.imageUrl.isNotBlank()) {
                AsyncImage(model = post.imageUrl, contentDescription = post.caption, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.78f)))))
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(16.dp), shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = 0.32f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = post.author, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = post.handle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                if (post.caption.isNotBlank()) Text(text = post.caption, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = post.stats, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.76f), modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionPill(Icons.Rounded.FavoriteBorder, "Like")
                    ActionPill(Icons.Rounded.Send, "Share")
                    ActionPill(Icons.Rounded.NorthEast, "Open")
                }
            }
        }
    }
}

// ── Stories ───────────────────────────────────────────────────────────────────

@Composable
private fun StorySceneCard(story: StorySceneUi) {
    Card(shape = RoundedCornerShape(34.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(660.dp)) {
            AsyncImage(model = story.backgroundUrl, contentDescription = story.author, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.74f)))))

            if (story.previewUrl.isNotBlank()) {
                AsyncImage(
                    model = story.previewUrl, contentDescription = "Story preview",
                    modifier = Modifier.padding(18.dp).size(width = 90.dp, height = 112.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.align(Alignment.TopStart).padding(start = 122.dp, top = 24.dp, end = 20.dp)) {
                Text(text = story.handle, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (story.caption.isNotBlank()) {
                    Text(text = story.caption, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
                story.reactions.forEach { reaction ->
                    StoryReactionBubble(reaction)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (story.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        story.tags.forEach { tag -> StoryTag(tag) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(text = story.views, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (story.location.isNotBlank()) {
                        Spacer(modifier = Modifier.width(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(text = story.location, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryReactionBubble(reaction: StoryReactionUi) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.52f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = reaction.avatarUrl, contentDescription = reaction.userHandle, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 10.dp)) {
                Text(text = reaction.userHandle, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = reaction.text, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
            if (reaction.emoji.isNotBlank()) Text(text = reaction.emoji, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun StoryTag(text: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF7A5A39).copy(alpha = 0.72f)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Shared micro components ───────────────────────────────────────────────────

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun InsightChip(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
}