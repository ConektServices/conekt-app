package com.conekt.suite.feature.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.LocalTrack
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

// ── Legacy UI model kept for demo data ───────────────────────────────────────

data class MusicTrackUi(
    val title: String, val artist: String,
    val coverUrl: String, val duration: String,
    val plays: String, val accent: Color
)

data class MusicArtistUi(val name: String, val imageUrl: String)

private enum class MusicHomeTab(val label: String, val icon: ImageVector) {
    HOME("Home",    Icons.Rounded.Home),
    SEARCH("Search", Icons.Rounded.Search),
    STREAM("Stream", Icons.Rounded.Bolt),
    LIBRARY("Local", Icons.Rounded.LibraryMusic)
}

private data class FriendListeningUi(
    val name: String, val avatarUrl: String,
    val track: MusicTrackUi, val status: String
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MusicScreen(
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val viewModel: MusicViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    val demoTracks  = remember { musicTracks() }
    val demoArtists = remember { musicArtists() }

    var selectedHomeTab by rememberSaveable { mutableStateOf(MusicHomeTab.HOME) }

    // Permission launcher (Android 13+ needs READ_MEDIA_AUDIO, older READ_EXTERNAL_STORAGE)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }

    LaunchedEffect(state.needsStoragePermission) {
        if (state.needsStoragePermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
            permissionLauncher.launch(perm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07080C), Color(0xFF0B0D12),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        if (state.activeTrack != null) {
            // ── Full player ────────────────────────────────────────────────────
            NowPlayingScreen(
                state     = state,
                demoQueue = demoTracks.map {
                    ActiveTrack(null, it.title, it.artist, "", it.coverUrl, it.coverUrl, 0L, MusicSource.ONLINE)
                },
                onBack          = { /* pop */ },
                onTogglePlay    = viewModel::togglePlayPause,
                onSeek          = viewModel::seekTo,
                onOpenComments  = { id -> id?.let { viewModel.openComments(it) } },
                viewModel       = viewModel
            )
        } else {
            // ── Home ───────────────────────────────────────────────────────────
            MusicHomeContent(
                state        = state,
                demoTracks   = demoTracks,
                demoArtists  = demoArtists,
                selectedTab  = selectedHomeTab,
                onTabSelected = { selectedHomeTab = it },
                onPlayOnline  = viewModel::playOnline,
                onPlayLocal   = viewModel::playLocal,
                onOpenShare   = viewModel::openShareSheet,
                onSearchChange = viewModel::onSearchQueryChange,
                viewModel    = viewModel
            )
        }

        // ── Sheets ─────────────────────────────────────────────────────────────
        ShareMusicSheet(
            state         = state.shareSheet,
            onTitleChange  = viewModel::onShareTitleChange,
            onArtistChange = viewModel::onShareArtistChange,
            onGenreChange  = viewModel::onShareGenreChange,
            onCoverPicked  = viewModel::onShareCoverPicked,
            onPublicToggle = viewModel::onSharePublicToggle,
            onSubmit       = viewModel::submitShare,
            onDismiss      = viewModel::closeShareSheet
        )

        MusicCommentsSheet(
            state          = state.commentsSheet,
            onDraftChange  = viewModel::onCommentDraftChange,
            onPostComment  = viewModel::postComment,
            onDismiss      = viewModel::closeComments
        )
    }
}

// ── Home content ──────────────────────────────────────────────────────────────

@Composable
private fun MusicHomeContent(
    state: MusicUiState,
    demoTracks: List<MusicTrackUi>,
    demoArtists: List<MusicArtistUi>,
    selectedTab: MusicHomeTab,
    onTabSelected: (MusicHomeTab) -> Unit,
    onPlayOnline: (MusicTrackRecord) -> Unit,
    onPlayLocal: (LocalTrack) -> Unit,
    onOpenShare: (LocalTrack) -> Unit,
    onSearchChange: (String) -> Unit,
    viewModel: MusicViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 162.dp, bottom = 210.dp
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            when (selectedTab) {
                MusicHomeTab.HOME -> {
                    // Stats card (real data)
                    if (!state.isLoadingStats) {
                        item { StatsCard(state.stats) }
                    }

                    // Online trending
                    if (state.onlineTracks.isNotEmpty()) {
                        item {
                            Column {
                                MusicSectionHeader("Trending on Conekt")
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(state.onlineTracks) { track ->
                                        OnlineTrackCard(track = track, onClick = { onPlayOnline(track) })
                                    }
                                }
                            }
                        }
                    } else {
                        // Demo fallback hero
                        item {
                            MusicHeroCard(track = demoTracks.first(), onClick = {})
                        }
                        item {
                            Column {
                                MusicSectionHeader("Trending now")
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(demoTracks) { track -> TrendingTrackCard(track = track, onClick = {}) }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            MusicSectionHeader("Popular artists")
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(demoArtists) { artist -> ArtistBubble(artist = artist) }
                            }
                        }
                    }

                    item {
                        Column {
                            MusicSectionHeader("For Conekt")
                            Spacer(modifier = Modifier.height(12.dp))
                            val tracksToShow = if (state.onlineTracks.isNotEmpty())
                                state.onlineTracks.take(4) else null
                            if (tracksToShow != null) {
                                tracksToShow.forEachIndexed { i, track ->
                                    OnlinePlaylistRow(track = track, onClick = { onPlayOnline(track) })
                                    if (i != tracksToShow.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                                }
                            } else {
                                demoTracks.forEachIndexed { i, track ->
                                    PlaylistRow(track = track, onClick = {})
                                    if (i != demoTracks.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }

                MusicHomeTab.SEARCH -> {
                    item {
                        SearchTabContent(
                            query         = state.searchQuery,
                            onQueryChange = onSearchChange,
                            results       = state.searchResults,
                            demoTracks    = demoTracks,
                            onPlayOnline  = onPlayOnline
                        )
                    }
                }

                MusicHomeTab.STREAM -> {
                    item { StreamTabContent(tracks = demoTracks, onOpenPlayer = {}) }
                }

                MusicHomeTab.LIBRARY -> {
                    item {
                        Column {
                            MusicSectionHeader("Your library")
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    if (state.isLoadingLocal) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp)
                            }
                        }
                    } else if (state.localTracks.isEmpty()) {
                        item {
                            EmptyLocalCard()
                        }
                    } else {
                        items(state.localTracks) { track ->
                            LocalTrackRow(
                                track    = track,
                                onClick  = { onPlayLocal(track) },
                                onShare  = { onOpenShare(track) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // My uploads section
                    if (state.myUploads.isNotEmpty()) {
                        item {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                MusicSectionHeader("My uploads")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(state.myUploads) { track ->
                            OnlinePlaylistRow(track = track, onClick = { onPlayOnline(track) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Top header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            MusicHeader()
            Spacer(modifier = Modifier.height(10.dp))
            MusicTopTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }

        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )
    }
}

// ── Stats card ────────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(stats: MusicStats) {
    Card(
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF12131A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.18f),
                            BrandEnd.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text       = "Your music stats",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label    = "Today",
                        value    = stats.todayPlays.toString(),
                        sub      = "plays",
                        accent   = BrandEnd
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label    = "Month",
                        value    = stats.monthlyPlays.toString(),
                        sub      = "plays",
                        accent   = InfoBlue
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label    = "Uploads",
                        value    = stats.topTrack?.playCount?.toString() ?: "0",
                        sub      = "total plays",
                        accent   = SuccessGreen
                    )
                }
                stats.topTrack?.let { top ->
                    Surface(
                        shape  = RoundedCornerShape(18.dp),
                        color  = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cover = top.coverUrl?.ifBlank { null }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandEnd.copy(alpha = 0.20f))
                            ) {
                                if (cover != null) {
                                    AsyncImage(
                                        model = cover, contentDescription = null,
                                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text  = "🔥 Top track",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandEnd
                                )
                                Text(
                                    text       = top.title,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Text(
                                    text  = "${top.playCount} plays • ${top.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.54f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    label: String, value: String, sub: String, accent: Color
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(18.dp),
        color    = accent.copy(alpha = 0.10f),
        border   = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = accent)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = sub, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.38f))
        }
    }
}

// ── Online track cards ────────────────────────────────────────────────────────

@Composable
private fun OnlineTrackCard(track: MusicTrackRecord, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.width(130.dp).clickable { onClick() },
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF12131A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandEnd.copy(alpha = 0.14f))
            ) {
                val cover = track.coverUrl?.ifBlank { null }
                if (cover != null) {
                    AsyncImage(
                        model = cover, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint               = BrandEnd.copy(alpha = 0.60f),
                        modifier           = Modifier.align(Alignment.Center).size(28.dp)
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    shape    = RoundedCornerShape(8.dp),
                    color    = Color.Black.copy(alpha = 0.54f)
                ) {
                    Text(
                        text     = "${track.playCount}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text     = track.title,
                style    = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color    = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text     = track.artist,
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.White.copy(alpha = 0.54f),
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun OnlinePlaylistRow(track: MusicTrackRecord, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(BrandEnd.copy(alpha = 0.14f))
            ) {
                val cover = track.coverUrl?.ifBlank { null }
                if (cover != null) {
                    AsyncImage(
                        model = cover, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = BrandEnd.copy(alpha = 0.60f),
                        modifier = Modifier.align(Alignment.Center).size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = track.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(
                text  = formatMs(track.durationMs.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Local track row ───────────────────────────────────────────────────────────

@Composable
private fun LocalTrackRow(track: LocalTrack, onClick: () -> Unit, onShare: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(BrandStart.copy(alpha = 0.14f))
            ) {
                val art = track.albumArtUri
                if (art != null) {
                    AsyncImage(
                        model = art, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint               = BrandStart.copy(alpha = 0.60f),
                        modifier           = Modifier.align(Alignment.Center).size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text       = track.title,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1
                )
                Text(
                    text  = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text  = formatMs(track.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Share button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ConektGradient.brandHorizontal)
                    .clickable { onShare() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Upload,
                    contentDescription = "Share",
                    tint               = Color.White,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyLocalCard() {
    Surface(
        shape  = RoundedCornerShape(24.dp),
        color  = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint               = BrandEnd.copy(alpha = 0.52f),
                modifier           = Modifier.size(40.dp)
            )
            Text(
                text       = "No local music found",
                style      = MaterialTheme.typography.titleSmall,
                color      = Color.White.copy(alpha = 0.60f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = "Music files on your device will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.34f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ── Now playing full screen ───────────────────────────────────────────────────

@Composable
private fun NowPlayingScreen(
    state: MusicUiState,
    demoQueue: List<ActiveTrack>,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenComments: (String?) -> Unit,
    viewModel: MusicViewModel
) {
    val track     = state.activeTrack ?: return
    val listState = rememberLazyListState()

    val collapseFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 520f).coerceIn(0f, 1f)
        }
    }

    val animatedCollapse by animateFloatAsState(targetValue = collapseFraction, label = "collapse")
    val albumHeight by animateDpAsState(targetValue = lerp(400.dp, 118.dp, animatedCollapse), label = "albumH")
    val blockSpacing by animateDpAsState(targetValue = lerp(18.dp, 8.dp, animatedCollapse), label = "spacing")
    val controlsAlpha by animateFloatAsState(targetValue = 1f - (animatedCollapse * 0.88f), label = "controlAlpha")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandEnd.copy(alpha = 0.18f),
                        Color(0xFF08090D), Color(0xFF06070A)
                    )
                )
            )
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 104.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(blockSpacing)
        ) {
            // Album art
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().height(albumHeight),
                    shape     = RoundedCornerShape(34.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFF101217)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val cover = track.coverUri?.ifBlank { null }
                        if (cover != null) {
                            AsyncImage(
                                model = cover, contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier         = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(BrandStart.copy(alpha = 0.40f), BrandEnd.copy(alpha = 0.30f)))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint               = Color.White.copy(alpha = 0.40f),
                                    modifier           = Modifier.size(64.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f)))
                            )
                        )
                    }
                }
            }

            // Track info + slider
            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (track.id != null) {
                                Icon(
                                    imageVector        = Icons.Rounded.ChatBubble,
                                    contentDescription = "Comments",
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.clickable { onOpenComments(track.id) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Slider(
                        value         = state.progressFraction,
                        onValueChange = onSeek,
                        colors        = SliderDefaults.colors(thumbColor = BrandEnd, activeTrackColor = BrandEnd, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = formatMs(state.positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = formatMs(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Controls
            item {
                Column(modifier = Modifier.graphicsLayer { alpha = controlsAlpha }) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipPrevious(demoQueue) }) {
                            Icon(imageVector = Icons.Rounded.FastRewind, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.size(78.dp).clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint               = Color.White,
                                modifier           = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { viewModel.skipNext(demoQueue) }) {
                            Icon(imageVector = Icons.Rounded.FastForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        for (icon in listOf(Icons.Rounded.Shuffle, Icons.AutoMirrored.Rounded.QueueMusic, Icons.Rounded.GraphicEq, Icons.Rounded.Repeat)) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
                                Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }

            // Source badge
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (track.source == MusicSource.LOCAL) BrandStart.copy(alpha = 0.16f) else BrandEnd.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (track.source == MusicSource.LOCAL) BrandStart.copy(alpha = 0.30f) else BrandEnd.copy(alpha = 0.28f))
                    ) {
                        Text(
                            text     = if (track.source == MusicSource.LOCAL) "LOCAL" else "STREAMING",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (track.source == MusicSource.LOCAL) BrandStart else BrandEnd,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (track.source == MusicSource.LOCAL) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text     = "Share online",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = BrandEnd,
                            modifier = Modifier.clickable {
                                // TODO: open share from player
                            }
                        )
                    }
                }
            }

            // Comments preview (online tracks only)
            if (track.id != null && state.commentsSheet.comments.isNotEmpty()) {
                item {
                    Text(text = "Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(state.commentsSheet.comments.take(3)) { c ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onOpenComments(track.id) },
                        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Text(text = (c.author.displayName ?: c.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(text = c.author.displayName ?: c.author.username, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = c.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (state.commentsSheet.comments.size > 3) {
                    item {
                        Text(
                            text     = "View all ${state.commentsSheet.comments.size} comments",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = BrandEnd,
                            modifier = Modifier.clickable { onOpenComments(track.id) }.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Top scrim
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(220.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.42f), Color.Transparent)))
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f), Color.Black.copy(alpha = 0.80f))))
        )

        Row(
            modifier          = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleButton(onClick = { viewModel.togglePlayPause() /* Back closes player */ }, icon = Icons.Rounded.KeyboardArrowDown, contentDescription = "Back")
            Spacer(modifier = Modifier.weight(1f))
            if (track.id != null) {
                GlassCircleButton(onClick = { onOpenComments(track.id) }, icon = Icons.Rounded.ChatBubble, contentDescription = "Comments")
                Spacer(modifier = Modifier.width(8.dp))
            }
            GlassCircleButton(onClick = {}, icon = Icons.Rounded.MoreHoriz, contentDescription = "More")
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun MusicHeader() {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Conekt Music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "listen in your connected space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(onClick = {}, icon = Icons.Rounded.Settings, contentDescription = "Settings")
            GlassCircleButton(onClick = {}, icon = Icons.Rounded.NotificationsNone, contentDescription = "Notifications")
        }
    }
}

@Composable
private fun MusicTopTabs(selectedTab: MusicHomeTab, onTabSelected: (MusicHomeTab) -> Unit) {
    Surface(
        modifier        = Modifier.padding(horizontal = 20.dp),
        shape           = RoundedCornerShape(24.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        shadowElevation = 14.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MusicHomeTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                            ))
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Search tab ────────────────────────────────────────────────────────────────

@Composable
private fun SearchTabContent(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<MusicTrackRecord>,
    demoTracks: List<MusicTrackUi>,
    onPlayOnline: (MusicTrackRecord) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(
            value = query, onValueChange = onQueryChange, singleLine = true,
            placeholder = { Text("Search songs, artists...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = BrandEnd
            )
        )

        if (query.isBlank()) {
            Column {
                MusicSectionHeader("Browse categories")
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SmallCategoryCard("Afrobeats", Icons.Rounded.Bolt, BrandEnd) }
                    item { SmallCategoryCard("Chill", Icons.Rounded.GraphicEq, InfoBlue) }
                    item { SmallCategoryCard("Workout", Icons.Rounded.FastForward, Color(0xFFFF8A5B)) }
                    item { SmallCategoryCard("Focus", Icons.Rounded.LibraryMusic, Color(0xFF8B5CF6)) }
                }
            }
        } else {
            if (results.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        Text("No results for \"$query\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { track ->
                        OnlinePlaylistRow(track = track, onClick = { onPlayOnline(track) })
                    }
                }
            }
        }
    }
}

// ── Kept demo-data composables (unchanged design) ─────────────────────────────

@Composable
private fun MusicHeroCard(track: MusicTrackUi, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().height(305.dp).clickable { onClick() },
        shape     = RoundedCornerShape(34.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = track.coverUrl, contentDescription = track.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.38f), Color.Black.copy(alpha = 0.82f)))))
            Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(track.accent.copy(alpha = 0.22f), Color.Transparent))))
            Surface(modifier = Modifier.padding(18.dp), shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.26f)) {
                Text(text = "CONEKT HITS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text(text = track.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = track.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.76f), modifier = Modifier.padding(top = 4.dp))
                Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onClick() }.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play now", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = track.plays, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.74f))
                }
            }
        }
    }
}

@Composable
private fun TrendingTrackCard(track: MusicTrackUi, onClick: () -> Unit) {
    Card(modifier = Modifier.width(122.dp).clickable { onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                AsyncImage(model = track.coverUrl, contentDescription = track.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.54f)))))
            }
            Text(text = track.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
            Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ArtistBubble(artist: MusicArtistUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(84.dp).background(ConektGradient.brandHorizontal, CircleShape).padding(2.dp).background(MaterialTheme.colorScheme.background, CircleShape).padding(3.dp)) {
            AsyncImage(model = artist.imageUrl, contentDescription = artist.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Text(text = artist.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun PlaylistRow(track: MusicTrackUi, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable { onClick() }, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), tonalElevation = 0.dp, shadowElevation = 6.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = track.coverUrl, contentDescription = track.title, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(text = track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
            Text(text = track.duration, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreamTabContent(tracks: List<MusicTrackUi>, onOpenPlayer: (MusicTrackUi) -> Unit) {
    val friends = listOf(
        FriendListeningUi("Elena Juni", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80", tracks[0], "streaming now"),
        FriendListeningUi("Daniel Moss", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80", tracks[1], "listening live"),
        FriendListeningUi("Nia Bloom", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=400&q=80", tracks[2], "sharing with friends")
    )
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(model = tracks[1].coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.76f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(text = "Friends are streaming", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "see what your circle is listening to right now", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f), modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Column {
            MusicSectionHeader("Streaming now")
            Spacer(modifier = Modifier.height(12.dp))
            friends.forEachIndexed { i, friend ->
                Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { onOpenPlayer(friend.track) }, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = friend.avatarUrl, contentDescription = friend.name, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(text = friend.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${friend.status} • ${friend.track.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                        AsyncImage(model = friend.track.coverUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                    }
                }
                if (i != friends.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SmallCategoryCard(title: String, icon: ImageVector, accent: Color) {
    Surface(modifier = Modifier.clickable { }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shadowElevation = 4.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GlassCircleButton(onClick: () -> Unit, icon: ImageVector, contentDescription: String) {
    Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f), shadowElevation = 10.dp, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Box(modifier = Modifier.clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MusicSectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

// ── Demo data ─────────────────────────────────────────────────────────────────

private fun musicTracks(): List<MusicTrackUi> = listOf(
    MusicTrackUi("Copines", "Aya Nakamura", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=900&q=80", "03:01", "2.1M listens", BrandEnd),
    MusicTrackUi("White Flag", "Bishop Briggs", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=900&q=80", "03:22", "1.3M listens", InfoBlue),
    MusicTrackUi("Courtesy Call", "Thousand Foot Krutch", "https://images.unsplash.com/photo-1501612780327-45045538702b?auto=format&fit=crop&w=900&q=80", "03:56", "980K listens", Color(0xFF8B5CF6)),
    MusicTrackUi("Who Gon Stop Me", "JAY-Z & Kanye West", "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=900&q=80", "04:15", "1.7M listens", Color(0xFFFF8A5B))
)

private fun musicArtists(): List<MusicArtistUi> = listOf(
    MusicArtistUi("Alan Walker",   "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=500&q=80"),
    MusicArtistUi("Katy Perry",    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80"),
    MusicArtistUi("Marshmello",    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=500&q=80"),
    MusicArtistUi("Demi Lovato",   "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=500&q=80")
)