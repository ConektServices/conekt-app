package com.conekt.suite.feature.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*

// ── Tab enum ──────────────────────────────────────────────────────────────────

private enum class MusicTab(val label: String, val icon: ImageVector) {
    HOME("Home",     Icons.Rounded.Home),
    SEARCH("Search", Icons.Rounded.Search),
    STREAM("Stream", Icons.Rounded.Bolt),
    LIBRARY("Local", Icons.Rounded.LibraryMusic)
}

// ─────────────────────────────────────────────────────────────────────────────
// MusicScreen
//
// The ViewModel passed in comes from MainActivity's activity-scoped instance,
// so state (activeTrack, isPlaying, etc.) is shared across the whole app.
// MusicScreen itself does NOT call viewModel() — the caller provides it.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MusicScreen(viewModel: MusicViewModel) {
    val state by viewModel.uiState.collectAsState()

    // Controls which tab is selected
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.HOME) }

    // Separate flag for Now Playing full screen — decoupled from tab state
    // so the close button can dismiss it without re-triggering it.
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // Auto-open Now Playing when a new track starts (user pressed play)
    // but only if we're already on the music screen.
    val currentTrackId = state.activeTrack?.uri
    LaunchedEffect(currentTrackId) {
        if (currentTrackId != null) {
            showNowPlaying = true
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onPermissionGranted() }

    LaunchedEffect(state.needsStoragePermission) {
        if (state.needsStoragePermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
            permissionLauncher.launch(perm)
        }
    }

    // Download toast
    state.downloadToastMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearDownloadToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07080C), Color(0xFF0B0D12), MaterialTheme.colorScheme.background)
                )
            )
    ) {
        // Now Playing takes over the full screen
        if (showNowPlaying && state.activeTrack != null) {
            NowPlayingFullScreen(
                state     = state,
                queue     = viewModel.buildQueue(),
                onDismiss = { showNowPlaying = false },
                viewModel = viewModel
            )
        } else {
            MusicHomeLayout(
                state         = state,
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it },
                onOpenNowPlaying = { showNowPlaying = true },
                viewModel     = viewModel
            )
        }

        // ── Bottom sheets ─────────────────────────────────────────────────────
        ShareMusicSheet(
            state          = state.shareSheet,
            onTitleChange  = viewModel::onShareTitleChange,
            onArtistChange = viewModel::onShareArtistChange,
            onGenreChange  = viewModel::onShareGenreChange,
            onCoverPicked  = viewModel::onShareCoverPicked,
            onPublicToggle = viewModel::onSharePublicToggle,
            onSubmit       = viewModel::submitShare,
            onDismiss      = viewModel::closeShareSheet
        )
        MusicCommentsSheet(
            state         = state.commentsSheet,
            onDraftChange = viewModel::onCommentDraftChange,
            onPostComment = viewModel::postComment,
            onDismiss     = viewModel::closeComments
        )
        if (state.showSettings) {
            MusicSettingsSheet(
                settings  = state.settings,
                onSave    = viewModel::onSettingsChange,
                onDismiss = viewModel::closeSettings
            )
        }

        // ── Download toast ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = state.downloadToastMessage != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 110.dp)
        ) {
            Surface(
                shape           = RoundedCornerShape(20.dp),
                color           = SuccessGreen.copy(alpha = 0.92f),
                shadowElevation = 18.dp
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(state.downloadToastMessage.orEmpty(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Home layout (tabs + content + mini player inside music screen) ─────────────

@Composable
private fun MusicHomeLayout(
    state:           MusicUiState,
    selectedTab:     MusicTab,
    onTabSelected:   (MusicTab) -> Unit,
    onOpenNowPlaying: () -> Unit,
    viewModel:       MusicViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Scrollable content ────────────────────────────────────────────────
        // NOTE: Search, Stream, Library tabs render items directly into the LazyColumn
        // (no nested Column wrapper) so there's no double-spacing.
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 20.dp, end = 20.dp,
                top    = 162.dp,
                // Extra bottom padding when mini player is visible so last item isn't hidden
                bottom = if (state.activeTrack != null) 210.dp else 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp) // items manage their own spacing
        ) {
            when (selectedTab) {
                MusicTab.HOME    -> homeTabItems(state, viewModel)
                MusicTab.SEARCH  -> searchTabItems(state, viewModel)
                MusicTab.STREAM  -> streamTabItems(state, viewModel)
                MusicTab.LIBRARY -> libraryTabItems(state, viewModel)
            }
        }

        // ── Top gradient scrim ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.40f), Color.Transparent)
                    )
                )
        )

        // ── Header + Tabs ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            MusicHeader(
                onSettingsClick = viewModel::openSettings,
                onRefreshClick  = viewModel::refresh
            )
            Spacer(Modifier.height(10.dp))
            MusicTabRow(selectedTab, onTabSelected)
        }

        // ── Bottom gradient scrim ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // ── Mini player inside Music screen ───────────────────────────────────
        // Sits above the app's bottom nav. Click opens Now Playing.
        AnimatedVisibility(
            visible  = state.activeTrack != null,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 74.dp) // above the bottom nav bar
        ) {
            state.activeTrack?.let { track ->
                MiniPlayer(
                    track     = track,
                    isPlaying = state.isPlaying,
                    progress  = state.progressFraction,
                    onExpand  = onOpenNowPlaying,          // tap → Now Playing
                    onToggle  = viewModel::togglePlayPause,
                    onNext    = { viewModel.skipNext(viewModel.buildQueue()) }
                )
            }
        }
    }
}

// ── HOME tab items ────────────────────────────────────────────────────────────

private fun LazyListScope.homeTabItems(state: MusicUiState, viewModel: MusicViewModel) {
    // Stats
    if (!state.isLoadingStats) {
        item(key = "stats") {
            StatsCard(state.stats)
            Spacer(Modifier.height(22.dp))
        }
    }

    // Online tracks
    if (state.isLoadingOnline) {
        item(key = "loading_online") {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(22.dp))
        }
    } else if (state.onlineTracks.isEmpty()) {
        item(key = "empty_online") {
            EmptyOnlineCard()
            Spacer(Modifier.height(22.dp))
        }
    } else {
        item(key = "trending") {
            SectionHeader("Trending on Conekt")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.onlineTracks.take(10), key = { "trend_${it.id}" }) { track ->
                    TrendingCard(track) { viewModel.playOnline(track) }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item(key = "for_you_header") {
            SectionHeader("For You")
            Spacer(Modifier.height(12.dp))
        }
        items(state.onlineTracks.take(5), key = { "foryou_${it.id}" }) { track ->
            OnlineTrackRow(track) { viewModel.playOnline(track) }
            Spacer(Modifier.height(8.dp))
        }
        item(key = "for_you_bottom") { Spacer(Modifier.height(14.dp)) }
    }

    // Live listeners
    if (state.liveListeners.isNotEmpty()) {
        item(key = "live_header") {
            SectionHeader("Live now")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.liveListeners.take(8), key = { "live_${it.userId}" }) { entry ->
                    LiveBubble(entry)
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

// ── SEARCH tab items ──────────────────────────────────────────────────────────

private fun LazyListScope.searchTabItems(state: MusicUiState, viewModel: MusicViewModel) {
    // Search bar
    item(key = "search_bar") {
        SearchBar(
            query         = state.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange
        )
        Spacer(Modifier.height(16.dp))
    }

    when {
        state.searchQuery.isBlank() -> {
            // Genre chips
            item(key = "genre_header") {
                SectionHeader("Browse genres")
                Spacer(Modifier.height(10.dp))
            }
            item(key = "genre_chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val genres = listOf(
                        "Afrobeats" to BrandEnd, "Chill" to InfoBlue,
                        "Workout"   to Color(0xFFFF8A5B), "Focus"  to Color(0xFF8B5CF6),
                        "Hip-Hop"   to SuccessGreen,      "R&B"    to Color(0xFFFFB14D),
                        "Electronic" to BrandStart,       "Jazz"   to Color(0xFF57F2C4)
                    )
                    items(genres, key = { it.first }) { (g, c) ->
                        GenreChip(g, c) { viewModel.onSearchQueryChange(g) }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            if (state.onlineTracks.isNotEmpty()) {
                item(key = "all_tracks_header") {
                    SectionHeader("All tracks")
                    Spacer(Modifier.height(12.dp))
                }
                items(state.onlineTracks, key = { "all_${it.id}" }) { track ->
                    OnlineTrackRow(track) { viewModel.playOnline(track) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        state.searchResults.isEmpty() -> {
            item(key = "no_results") {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Text(
                            "No results for \"${state.searchQuery}\"",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        else -> {
            item(key = "results_header") {
                SectionHeader("${state.searchResults.size} results")
                Spacer(Modifier.height(12.dp))
            }
            items(state.searchResults, key = { "res_${it.id}" }) { track ->
                OnlineTrackRow(track) { viewModel.playOnline(track) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── STREAM tab items ──────────────────────────────────────────────────────────

private fun LazyListScope.streamTabItems(state: MusicUiState, viewModel: MusicViewModel) {
    item(key = "stream_hero") {
        StreamHeroCard()
        Spacer(Modifier.height(18.dp))
    }

    if (state.liveListeners.isEmpty()) {
        item(key = "stream_empty") {
            Surface(
                shape  = RoundedCornerShape(24.dp),
                color  = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f), modifier = Modifier.size(40.dp))
                    Text("Nobody streaming right now", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("When friends listen to public tracks, they'll appear here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f), textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        item(key = "stream_count") {
            SectionHeader("Now Streaming · ${state.liveListeners.size}")
            Spacer(Modifier.height(12.dp))
        }
        items(state.liveListeners, key = { "stream_${it.userId}_${it.trackId}" }) { entry ->
            LiveListenerCard(
                entry         = entry,
                isDownloading = state.downloadingTrackId == entry.trackId,
                onPlay        = {
                    val track = state.onlineTracks.find { it.id == entry.trackId }
                    if (track != null) viewModel.playOnline(track)
                },
                onDownload    = { viewModel.downloadTrack(entry) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ── LIBRARY tab items ─────────────────────────────────────────────────────────

private fun LazyListScope.libraryTabItems(state: MusicUiState, viewModel: MusicViewModel) {
    item(key = "lib_header") {
        SectionHeader("Your library")
        Spacer(Modifier.height(12.dp))
    }

    if (state.isLoadingLocal) {
        item(key = "lib_loading") {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp)
            }
        }
    } else if (state.localTracks.isEmpty()) {
        item(key = "lib_empty") { EmptyLocalCard() }
    } else {
        items(state.localTracks, key = { "local_${it.id}" }) { track ->
            LocalTrackRow(
                track     = track,
                isPlaying = state.activeTrack?.uri == track.uri && state.isPlaying,
                onClick   = { viewModel.playLocal(track) },
                onShare   = { viewModel.openShareSheet(track) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (state.myUploads.isNotEmpty()) {
        item(key = "uploads_header") {
            Spacer(Modifier.height(8.dp))
            SectionHeader("My uploads")
            Spacer(Modifier.height(12.dp))
        }
        items(state.myUploads, key = { "upload_${it.id}" }) { track ->
            OnlineTrackRow(track) { viewModel.playOnline(track) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun MusicHeader(onSettingsClick: () -> Unit, onRefreshClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Conekt Music",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "your connected sound space",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassBtn(Icons.Rounded.Refresh,  "Refresh",  onRefreshClick)
            GlassBtn(Icons.Rounded.Settings, "Settings", onSettingsClick)
        }
    }
}

@Composable
private fun MusicTabRow(selected: MusicTab, onSelect: (MusicTab) -> Unit) {
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
            MusicTab.entries.forEach { tab ->
                val sel = tab == selected
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (sel) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 4.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint     = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        tab.label,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Stats card ────────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(stats: com.conekt.suite.data.model.MusicStats) {
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
                        listOf(BrandStart.copy(alpha = 0.18f), BrandEnd.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Your music stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(Modifier.weight(1f), "Today",  stats.todayPlays.toString(),                         "plays",      BrandEnd)
                    StatPill(Modifier.weight(1f), "Month",  stats.monthlyPlays.toString(),                       "plays",      InfoBlue)
                    StatPill(Modifier.weight(1f), "Top",    stats.topTrack?.playCount?.toString() ?: "—",         "plays",      SuccessGreen)
                }
                stats.topTrack?.let { top ->
                    Surface(
                        shape  = RoundedCornerShape(18.dp),
                        color  = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandEnd.copy(alpha = 0.20f))
                            ) {
                                top.coverUrl?.ifBlank { null }?.let {
                                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("🔥 Most played upload", style = MaterialTheme.typography.labelSmall, color = BrandEnd)
                                Text(top.title,  style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${top.playCount} plays · ${top.artist}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.54f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(modifier: Modifier, label: String, value: String, sub: String, accent: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = 0.10f), border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(sub,   style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.38f))
        }
    }
}

// ── Trending card ─────────────────────────────────────────────────────────────

@Composable
private fun TrendingCard(track: com.conekt.suite.data.model.MusicTrackRecord, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.width(132.dp).clickable { onClick() },
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF12131A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandEnd.copy(alpha = 0.14f))
            ) {
                track.coverUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd.copy(alpha = 0.60f), modifier = Modifier.align(Alignment.Center).size(28.dp))

                Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.60f)) {
                    Text("${track.playCount}", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(track.title,  style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.54f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ── Online track row ──────────────────────────────────────────────────────────

@Composable
private fun OnlineTrackRow(track: com.conekt.suite.data.model.MusicTrackRecord, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(BrandEnd.copy(alpha = 0.14f))) {
                track.coverUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd.copy(alpha = 0.60f), modifier = Modifier.align(Alignment.Center).size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(track.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${track.artist}${track.album?.let { " · $it" } ?: ""}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMs(track.durationMs.toLong()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = BrandEnd, modifier = Modifier.size(12.dp))
                    Text("${track.playCount}", style = MaterialTheme.typography.labelSmall, color = BrandEnd)
                }
            }
        }
    }
}

// ── Local track row ───────────────────────────────────────────────────────────

@Composable
private fun LocalTrackRow(
    track:     com.conekt.suite.data.model.LocalTrack,
    isPlaying: Boolean,
    onClick:   () -> Unit,
    onShare:   () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onClick() },
        color    = if (isPlaying) BrandEnd.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border   = BorderStroke(1.dp, if (isPlaying) BrandEnd.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(BrandStart.copy(alpha = 0.14f))) {
                track.albumArtUri?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandStart.copy(alpha = 0.60f), modifier = Modifier.align(Alignment.Center).size(22.dp))
                if (isPlaying) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.30f)), contentAlignment = Alignment.Center) {
                        PulsingWaveIndicator()
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(track.title,  fontWeight = FontWeight.SemiBold, color = if (isPlaying) BrandEnd else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(formatMs(track.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onShare() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Upload, "Share", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Live bubble ───────────────────────────────────────────────────────────────

@Composable
private fun LiveBubble(entry: LiveListenerEntry) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(ConektGradient.brandHorizontal)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
            ) {
                if (entry.avatarUrl != null) {
                    AsyncImage(entry.avatarUrl, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
                        Text(
                            (entry.displayName ?: entry.username).first().uppercaseChar().toString(),
                            color = BrandEnd, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Box(Modifier.size(16.dp).clip(CircleShape).background(SuccessGreen).border(2.dp, MaterialTheme.colorScheme.background, CircleShape))
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.displayName ?: entry.username, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(entry.trackTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value         = query,
        onValueChange = onQueryChange,
        singleLine    = true,
        placeholder   = { Text("Search songs, artists, albums…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon   = { Icon(Icons.Rounded.Search, null) },
        trailingIcon  = if (query.isNotBlank()) ({
            Icon(Icons.Rounded.Close, null, modifier = Modifier.clickable { onQueryChange("") })
        }) else null,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(22.dp),
        colors        = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor             = BrandEnd
        )
    )
}

@Composable
private fun GenreChip(label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape    = RoundedCornerShape(18.dp),
        color    = accent.copy(alpha = 0.14f),
        border   = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

// ── Stream hero ───────────────────────────────────────────────────────────────

@Composable
private fun StreamHeroCard() {
    Card(
        shape     = RoundedCornerShape(30.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF12131A)),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.22f), BrandEnd.copy(alpha = 0.14f))))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                    Text("Live Stream", style = MaterialTheme.typography.labelMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
                Text(
                    "What your circle is\nlistening to right now",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ── Live listener card ────────────────────────────────────────────────────────

@Composable
private fun LiveListenerCard(
    entry:         LiveListenerEntry,
    isDownloading: Boolean,
    onPlay:        () -> Unit,
    onDownload:    () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar + online dot
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.18f))) {
                    if (entry.avatarUrl != null) {
                        AsyncImage(entry.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                (entry.displayName ?: entry.username).first().uppercaseChar().toString(),
                                style      = MaterialTheme.typography.titleMedium,
                                color      = BrandEnd,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(Modifier.size(13.dp).clip(CircleShape).background(SuccessGreen).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape))
            }

            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(entry.displayName ?: entry.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(SuccessGreen))
                    Text(
                        "${entry.trackTitle} · ${entry.artistName}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.isLocal) {
                    Surface(shape = RoundedCornerShape(8.dp), color = BrandStart.copy(alpha = 0.14f), modifier = Modifier.padding(top = 3.dp)) {
                        Text("Local", style = MaterialTheme.typography.labelSmall, color = BrandStart, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                }
            }

            // Cover thumbnail
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(BrandEnd.copy(alpha = 0.12f))) {
                entry.trackCoverUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd.copy(alpha = 0.60f), modifier = Modifier.align(Alignment.Center).size(18.dp))
            }

            Spacer(Modifier.width(8.dp))

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(ConektGradient.brandHorizontal).clickable { onPlay() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(17.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), CircleShape)
                        .clickable(enabled = !isDownloading) { onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), color = BrandEnd, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Download, "Download", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MiniPlayer  (public — used by MainActivity for other screens too)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniPlayer(
    track:    ActiveTrack,
    isPlaying: Boolean,
    progress:  Float,
    onExpand:  () -> Unit,   // tap → open Now Playing
    onToggle:  () -> Unit,
    onNext:    () -> Unit,
    modifier:  Modifier = Modifier
) {
    Surface(
        modifier        = modifier.fillMaxWidth().clickable { onExpand() },
        shape           = RoundedCornerShape(22.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation  = 0.dp,
        shadowElevation = 28.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column {
            // Progress bar at very top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(ConektGradient.brandHorizontal)
                )
            }

            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover art
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandEnd.copy(alpha = 0.14f))
                ) {
                    track.coverUri?.ifBlank { null }?.let {
                        AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } ?: Icon(
                        Icons.Rounded.MusicNote, null,
                        tint     = BrandEnd.copy(alpha = 0.60f),
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }

                // Title + artist
                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(
                        track.title,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        style      = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        track.artist,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Play/pause — intercept click so it doesn't also trigger onExpand
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ConektGradient.brandHorizontal)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "Toggle",
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Next
                Box(
                    Modifier.size(38.dp).clip(CircleShape).clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Now Playing — full screen
// onDismiss = close and go back to mini player (track keeps playing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingFullScreen(
    state:     MusicUiState,
    queue:     List<ActiveTrack>,
    onDismiss: () -> Unit,   // ← arrow-down button: just hides the screen, music continues
    viewModel: MusicViewModel
) {
    val track     = state.activeTrack ?: return
    val listState = rememberLazyListState()

    val collapseFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 480f).coerceIn(0f, 1f)
        }
    }
    val animCollapse by animateFloatAsState(collapseFraction, label = "collapse")
    val albumHeight  by animateDpAsState(lerp(360.dp, 100.dp, animCollapse), label = "albumH")

    // Build "up next" — everything after the currently playing track, no cap
    val upNextQueue = remember(queue, track.uri) {
        val idx = queue.indexOfFirst { it.uri == track.uri }
        if (idx >= 0) queue.drop(idx + 1) else queue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BrandEnd.copy(alpha = 0.20f), Color(0xFF08090D), Color(0xFF06070A))
                )
            )
    ) {
        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 22.dp, end = 22.dp, top = 100.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Album art (collapses as user scrolls)
            item(key = "album_art") {
                Card(
                    Modifier.fillMaxWidth().height(albumHeight),
                    shape     = RoundedCornerShape(32.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFF101217)),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        val cover = track.coverUri?.ifBlank { null }
                        if (cover != null) {
                            AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(BrandStart.copy(alpha = 0.40f), BrandEnd.copy(alpha = 0.30f)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(64.dp))
                            }
                        }
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f)))))
                    }
                }
            }

            // Track info + seek bar
            item(key = "track_info") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title,  style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(track.artist, style = MaterialTheme.typography.bodyMedium,    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (track.id != null) {
                                Icon(
                                    Icons.Rounded.ChatBubble, "Comments",
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { viewModel.openComments(track.id) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Slider(
                        value         = state.progressFraction,
                        onValueChange = viewModel::seekTo,
                        colors        = SliderDefaults.colors(thumbColor = BrandEnd, activeTrackColor = BrandEnd, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text(formatMs(state.positionMs),   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text(formatMs(track.durationMs),   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Playback controls
            item(key = "controls") {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipPrevious(queue) }) {
                            Icon(Icons.Rounded.FastRewind, "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Box(
                            Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(ConektGradient.brandHorizontal)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                "Play/Pause",
                                tint     = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { viewModel.skipNext(queue) }) {
                            Icon(Icons.Rounded.FastForward, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        for (icon in listOf(Icons.Rounded.Shuffle, Icons.AutoMirrored.Rounded.QueueMusic, Icons.Rounded.GraphicEq, Icons.Rounded.Repeat)) {
                            Surface(
                                Modifier.size(44.dp), CircleShape,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Source badge
            item(key = "source") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = if (track.source == MusicSource.LOCAL) BrandStart.copy(alpha = 0.16f) else BrandEnd.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (track.source == MusicSource.LOCAL) BrandStart.copy(alpha = 0.30f) else BrandEnd.copy(alpha = 0.28f))
                    ) {
                        Text(
                            if (track.source == MusicSource.LOCAL) "LOCAL" else "STREAMING",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = if (track.source == MusicSource.LOCAL) BrandStart else BrandEnd,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (track.source == MusicSource.LOCAL) {
                        Text("Share online", style = MaterialTheme.typography.labelMedium, color = BrandEnd, modifier = Modifier.clickable {})
                    }
                }
            }

            // ── Queue — FULL list, no cap ─────────────────────────────────────
            item(key = "queue_header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Up Next", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    if (upNextQueue.isNotEmpty()) {
                        Text("${upNextQueue.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (upNextQueue.isEmpty()) {
                item(key = "queue_empty") {
                    Text("No more tracks in queue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // All tracks, no take(10) limit — LazyColumn handles virtualisation
                items(upNextQueue, key = { "q_${it.uri}" }) { qTrack ->
                    QueueRow(qTrack) {
                        if (qTrack.source == MusicSource.LOCAL) {
                            viewModel.playLocal(
                                com.conekt.suite.data.model.LocalTrack(
                                    0, qTrack.title, qTrack.artist, qTrack.album,
                                    qTrack.durationMs, qTrack.uri, qTrack.coverUri
                                )
                            )
                        } else {
                            state.onlineTracks.find { it.id == qTrack.id }?.let { viewModel.playOnline(it) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Comments preview
            if (track.id != null && state.commentsSheet.comments.isNotEmpty()) {
                item(key = "comments_header") {
                    Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(state.commentsSheet.comments.take(3), key = { "c_${it.id}" }) { c ->
                    Surface(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { viewModel.openComments(track.id) },
                        color  = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                                Text((c.author.displayName ?: c.author.username).first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(c.author.displayName ?: c.author.username, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(c.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Gradient scrims
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.42f), Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f), Color.Black.copy(alpha = 0.80f)))))

        // Top row: arrow-down (dismiss → back to mini player) + comments + more
        Row(
            modifier          = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ← THIS is the fix: clicking down-arrow calls onDismiss, which sets
            //   showNowPlaying = false. Music keeps playing; mini player reappears.
            GlassBtn(Icons.Rounded.KeyboardArrowDown, "Dismiss", onDismiss)
            Spacer(Modifier.weight(1f))
            if (track.id != null) {
                GlassBtn(Icons.Rounded.ChatBubble, "Comments") { viewModel.openComments(track.id) }
                Spacer(Modifier.width(8.dp))
            }
            GlassBtn(Icons.Rounded.MoreHoriz, "More") {}
        }
    }
}

@Composable
private fun QueueRow(track: ActiveTrack, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        color  = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(BrandEnd.copy(alpha = 0.12f))) {
                track.coverUri?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd.copy(alpha = 0.50f), modifier = Modifier.align(Alignment.Center).size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(track.title,  style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(formatMs(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyOnlineCard() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.MusicOff, null, tint = BrandEnd.copy(alpha = 0.50f), modifier = Modifier.size(40.dp))
            Text("No tracks on Conekt yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text("Be the first to upload and share your music!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyLocalCard() {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.04f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.LibraryMusic, null, tint = BrandEnd.copy(alpha = 0.52f), modifier = Modifier.size(40.dp))
            Text("No local music found", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.60f), fontWeight = FontWeight.SemiBold)
            Text("Music files on your device will appear here", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.34f), textAlign = TextAlign.Center)
        }
    }
}

// ── Pulsing wave indicator ────────────────────────────────────────────────────

@Composable
private fun PulsingWaveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val height by infiniteTransition.animateFloat(
                initialValue  = 4f,
                targetValue   = 14f,
                animationSpec = infiniteRepeatable(tween(300 + i * 80), RepeatMode.Reverse),
                label         = "bar$i"
            )
            Box(Modifier.width(3.dp).height(height.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
        }
    }
}

// ── Settings sheet ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSettingsSheet(
    settings:  MusicSettings,
    onSave:    (MusicSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var s by remember { mutableStateOf(settings) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color(0xFF0D0E14),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.20f)))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Music Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("personalise your sound experience", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.48f), modifier = Modifier.padding(top = 4.dp))
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
                }
            }

            SettingsSectionLabel("Privacy & Activity")
            SettingsToggle("Share listening activity", "Let friends see what you're streaming",               s.shareListeningActivity, Icons.Rounded.Visibility)   { s = s.copy(shareListeningActivity = it) }
            SettingsToggle("Share local plays",        "Broadcast when you play local tracks",               s.shareLocalActivity,     Icons.Rounded.Share)         { s = s.copy(shareLocalActivity = it) }
            SettingsToggle("Allow track downloads",    "Others can download music you uploaded",             s.allowDownloads,         Icons.Rounded.Download)      { s = s.copy(allowDownloads = it) }
            SettingsToggle("Live notifications",       "Notify me when friends start streaming",            s.liveNotifications,      Icons.Rounded.NotificationsNone) { s = s.copy(liveNotifications = it) }

            SettingsSectionLabel("Playback")
            SettingsToggle("Autoplay",                 "Automatically play next track",                     s.autoplay,               Icons.Rounded.PlayArrow)     { s = s.copy(autoplay = it) }
            SettingsToggle("High quality stream",      "Better audio, uses more data",                      s.highQualityStream,      Icons.Rounded.HighQuality)   { s = s.copy(highQualityStream = it) }
            SettingsToggle("Equalizer visualization",  "Show waveform on Now Playing",                      s.showEqualizer,          Icons.Rounded.GraphicEq)     { s = s.copy(showEqualizer = it) }

            SettingsSectionLabel("Cross-fade")
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.06f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cross-fade between tracks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(if (s.crossfadeSeconds == 0) "Off" else "${s.crossfadeSeconds}s", style = MaterialTheme.typography.labelMedium, color = BrandEnd)
                    }
                    Slider(
                        value         = s.crossfadeSeconds.toFloat(),
                        onValueChange = { s = s.copy(crossfadeSeconds = it.toInt()) },
                        valueRange    = 0f..10f,
                        steps         = 9,
                        colors        = SliderDefaults.colors(thumbColor = BrandEnd, activeTrackColor = BrandEnd, inactiveTrackColor = Color.White.copy(alpha = 0.18f)),
                        modifier      = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ConektGradient.brandHorizontal)
                    .clickable { onSave(s); onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Save settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.44f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, icon: ImageVector, onToggle: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.06f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(if (checked) BrandEnd.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (checked) BrandEnd else Color.White.copy(alpha = 0.40f), modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title,    style = MaterialTheme.typography.titleSmall,  fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,   color = Color.White.copy(alpha = 0.46f), modifier = Modifier.padding(top = 2.dp))
            }
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = BrandEnd,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
                )
            )
        }
    }
}

// ── Shared small helpers ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
internal fun GlassBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        Modifier.size(42.dp), CircleShape,
        MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        shadowElevation = 10.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(Modifier.clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

internal fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}