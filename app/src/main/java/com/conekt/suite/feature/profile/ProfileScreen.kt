package com.conekt.suite.feature.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.data.model.FileRecord
import com.conekt.suite.data.model.NoteRecord
import com.conekt.suite.data.model.PostRecord
import com.conekt.suite.data.model.ProfileRecord
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

// ── Tab enum ──────────────────────────────────────────────────────────────────

private enum class ProfileTab { OVERVIEW, POSTS, FILES }

// ── Lightweight UI models ─────────────────────────────────────────────────────

private data class ProfileStatUi(val label: String, val value: String)

private data class ProfileFileUi(
    val name: String,
    val meta: String,
    val type: String,
    val accent: Color,
    val icon: ImageVector
)

private data class ProfileNoteUi(
    val title: String,
    val body: String,
    val isPublic: Boolean
)

private data class ProfilePostUi(
    val imageUrl: String,
    val caption: String,
    val likes: Int,
    val comments: Int
)

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun FileRecord.toUi() = ProfileFileUi(
    name  = name,
    meta  = "${mimeType.substringBefore("/").replaceFirstChar { it.uppercaseChar() }} • ${formatBytes(sizeBytes)}",
    type  = fileType.replaceFirstChar { it.uppercaseChar() },
    accent = when (fileType) {
        "image"    -> InfoBlue
        "audio"    -> SuccessGreen
        "video"    -> Color(0xFFFF8A5B)
        "document" -> BrandEnd
        else       -> Color(0xFF9B4DFF)
    },
    icon = when (fileType) {
        "image"    -> Icons.Rounded.Image
        "audio"    -> Icons.Rounded.Headphones
        "video"    -> Icons.Rounded.PlayArrow
        "document" -> Icons.Rounded.Description
        else       -> Icons.Rounded.InsertDriveFile
    }
)

private fun NoteRecord.toUi() = ProfileNoteUi(
    title    = title,
    body     = body.take(120),
    isPublic = isSharedPost
)

private fun PostRecord.toUi() = ProfilePostUi(
    imageUrl = mediaUrls.firstOrNull().orEmpty(),
    caption  = body.orEmpty().take(140),
    likes    = likeCount,
    comments = commentCount
)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1_024         -> "$bytes B"
    bytes < 1_048_576     -> "${"%.1f".format(bytes / 1_024f)} KB"
    bytes < 1_073_741_824 -> "${"%.1f".format(bytes / 1_048_576f)} MB"
    else                  -> "${"%.2f".format(bytes / 1_073_741_824f)} GB"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.OVERVIEW) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06070A),
                        Color(0xFF0A0B10),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandEnd
                )
            }

            state.profile == null -> {
                // Could not load profile — show friendly error
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = state.errorMessage ?: "Could not load profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ConektGradient.brandHorizontal)
                            .clickable { viewModel.refresh() }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            else -> {
                val profile    = state.profile!!
                val fileUis    = state.files.map { it.toUi() }
                val noteUis    = state.notes.map { it.toUi() }
                val postUis    = state.posts
                    .filter { it.mediaUrls.isNotEmpty() }
                    .map { it.toUi() }

                val stats = listOf(
                    ProfileStatUi("Posts",     state.posts.size.toString()),
                    ProfileStatUi("Followers", profile.followerCount.toString()),
                    ProfileStatUi("Following", profile.followingCount.toString()),
                    ProfileStatUi("Storage",   formatBytes(profile.storageUsedBytes))
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 178.dp, bottom = 176.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        ProfileHeroCard(
                            profile     = profile,
                            stats       = stats,
                            onEditClick = onEditClick
                        )
                    }

                    item {
                        ProfileTopTabs(
                            selectedTab   = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    when (selectedTab) {
                        ProfileTab.OVERVIEW -> {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PlanBadgeCard(modifier = Modifier.weight(1f))
                                    StorageCard(
                                        modifier   = Modifier.weight(1f),
                                        used       = profile.storageUsedBytes,
                                        limit      = profile.storageLimitBytes
                                    )
                                }
                            }
                            if (fileUis.isNotEmpty()) {
                                item { FilesSection(files = fileUis.take(3)) }
                            }
                            if (noteUis.isNotEmpty()) {
                                item { NotesSection(notes = noteUis) }
                            }
                            if (postUis.isNotEmpty()) {
                                item { PostsSection(posts = postUis.take(2)) }
                            }
                            if (fileUis.isEmpty() && noteUis.isEmpty() && postUis.isEmpty()) {
                                item { EmptyCard(onEditClick = onEditClick) }
                            }
                        }

                        ProfileTab.POSTS -> {
                            if (noteUis.isNotEmpty()) item { NotesSection(notes = noteUis) }
                            if (postUis.isNotEmpty()) item { PostsSection(posts = postUis) }
                            if (noteUis.isEmpty() && postUis.isEmpty()) {
                                item { EmptyTextCard("No posts yet. Share your first post from the Feed.") }
                            }
                        }

                        ProfileTab.FILES -> {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PlanBadgeCard(modifier = Modifier.weight(1f))
                                    StorageCard(
                                        modifier = Modifier.weight(1f),
                                        used     = profile.storageUsedBytes,
                                        limit    = profile.storageLimitBytes
                                    )
                                }
                            }
                            if (fileUis.isNotEmpty()) item { FilesSection(files = fileUis) }
                            else item { EmptyTextCard("No files yet. Upload from the Vault.") }
                        }
                    }
                }
            }
        }

        // ── Overlays ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(290.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.84f),
                            Color.Black.copy(alpha = 0.46f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBtn(Icons.Rounded.ArrowBack, "Back", onBackClick)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text  = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "identity, notes, vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassBtn(Icons.Rounded.Edit, "Edit profile", onEditClick)
                GlassBtn(Icons.Rounded.MoreHoriz, "More", {})
            }
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroCard(
    profile: ProfileRecord,
    stats: List<ProfileStatUi>,
    onEditClick: () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(34.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {

            // Banner
            val bannerUrl = profile.bannerUrl?.ifBlank { null }
            if (bannerUrl != null) {
                AsyncImage(
                    model            = bannerUrl,
                    contentDescription = "Banner",
                    modifier         = Modifier.fillMaxSize(),
                    contentScale     = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    BrandStart.copy(alpha = 0.28f),
                                    BrandEnd.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.38f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Edit button top-right
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clickable { onEditClick() },
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.34f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint               = Color.White,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(text = "Edit", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }

            // Bottom info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    val avatarUrl = profile.avatarUrl?.ifBlank { null }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model            = avatarUrl,
                                contentDescription = "Avatar",
                                modifier         = Modifier.fillMaxSize(),
                                contentScale     = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ConektGradient.brandHorizontal),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = profile.displayName
                                        ?.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: profile.username.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: "?",
                                    style      = MaterialTheme.typography.headlineSmall,
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = profile.displayName?.ifBlank { null }
                                    ?: profile.username,
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            if (profile.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector        = Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint               = BrandEnd,
                                    modifier           = Modifier.size(20.dp)
                                )
                            }
                        }
                        val locationSuffix = profile.location?.ifBlank { null }?.let { " • $it" } ?: ""
                        Text(
                            text  = "@${profile.username}$locationSuffix",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.76f)
                        )
                    }
                }

                val bio = profile.bio?.ifBlank { null }
                if (bio != null) {
                    Text(
                        text     = bio,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(stats) { stat -> StatPill(stat) }
                }
            }
        }
    }
}

// ── Tab bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTopTabs(selectedTab: ProfileTab, onTabSelected: (ProfileTab) -> Unit) {
    Surface(
        shape         = RoundedCornerShape(24.dp),
        color         = MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
        shadowElevation = 12.dp,
        border        = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProfileTab.entries.forEach { tab ->
                val selected = tab == selectedTab
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
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = tab.name,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Stat pill ─────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(stat: ProfileStatUi) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier             = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Text(
                text       = stat.value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text  = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

// ── Plan / Storage cards ──────────────────────────────────────────────────────

@Composable
private fun PlanBadgeCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFD86B).copy(alpha = 0.22f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFD86B).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Plan",
                        tint               = Color(0xFFFFD86B)
                    )
                }
                Text(
                    text       = "Free plan",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(top = 14.dp)
                )
                Text(
                    text     = "Upgrade for more storage.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                MiniChip("Active")
            }
        }
    }
}

@Composable
private fun StorageCard(modifier: Modifier = Modifier, used: Long, limit: Long) {
    val fraction = if (limit > 0L) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(InfoBlue.copy(alpha = 0.16f), MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InfoBlue.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Storage,
                        contentDescription = "Storage",
                        tint               = InfoBlue
                    )
                }
                Text(
                    text       = "Storage",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(top = 14.dp)
                )
                Text(
                    text     = "${formatBytes(used)} of ${formatBytes(limit)}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .background(ConektGradient.brandHorizontal)
                    )
                }
            }
        }
    }
}

// ── Files section ─────────────────────────────────────────────────────────────

@Composable
private fun FilesSection(files: List<ProfileFileUi>) {
    Column {
        SectionTitle("Vault")
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape     = RoundedCornerShape(30.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                files.forEachIndexed { i, file ->
                    FileRow(file)
                    if (i != files.lastIndex) Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: ProfileFileUi) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable {},
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(file.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = file.icon, contentDescription = file.name, tint = file.accent)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text       = file.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = file.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = file.accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text     = file.type,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = file.accent
                )
            }
        }
    }
}

// ── Notes section ─────────────────────────────────────────────────────────────

@Composable
private fun NotesSection(notes: List<ProfileNoteUi>) {
    Column {
        SectionTitle("Notes")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(notes) { note -> NoteCard(note) }
        }
    }
}

@Composable
private fun NoteCard(note: ProfileNoteUi) {
    Card(
        modifier  = Modifier.width(280.dp).height(190.dp),
        shape     = RoundedCornerShape(30.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.12f),
                            BrandEnd.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniChip("note")
                    MiniChip(
                        text = if (note.isPublic) "Public" else "Private",
                        icon = if (note.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock
                    )
                }
                Text(
                    text       = note.title,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(top = 14.dp)
                )
                if (note.body.isNotBlank()) {
                    Text(
                        text     = note.body,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint               = BrandEnd,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text  = "Note post",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Posts section ─────────────────────────────────────────────────────────────

@Composable
private fun PostsSection(posts: List<ProfilePostUi>) {
    Column {
        SectionTitle("Posts")
        Spacer(modifier = Modifier.height(12.dp))
        posts.forEachIndexed { i, post ->
            PostCard(post)
            if (i != posts.lastIndex) Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PostCard(post: ProfilePostUi) {
    Card(
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
        ) {
            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model              = post.imageUrl,
                    contentDescription = post.caption,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.06f),
                                Color.Black.copy(alpha = 0.26f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                if (post.caption.isNotBlank()) {
                    Text(
                        text       = post.caption,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
                Text(
                    text     = "${post.likes} likes • ${post.comments} comments",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    modifier              = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPill(Icons.Rounded.FavoriteBorder, "Like")
                    ActionPill(Icons.Rounded.EditNote, "Note")
                }
            }
        }
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyCard(onEditClick: () -> Unit) {
    Card(
        shape     = RoundedCornerShape(30.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.10f),
                            BrandEnd.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier         = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(ConektGradient.brandHorizontal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(30.dp)
                    )
                }
                Text(
                    text       = "Your space is empty",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(top = 16.dp)
                )
                Text(
                    text        = "Complete your profile, upload files to the Vault, or write your first note in Canvas.",
                    style       = MaterialTheme.typography.bodyMedium,
                    color       = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier    = Modifier.padding(top = 8.dp),
                    textAlign   = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(ConektGradient.brandHorizontal)
                        .clickable { onEditClick() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text       = "Complete profile",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTextCard(message: String) {
    Surface(
        shape  = RoundedCornerShape(24.dp),
        color  = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Micro components ──────────────────────────────────────────────────────────

@Composable
private fun ActionPill(icon: ImageVector, label: String) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = Color.Black.copy(alpha = 0.26f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun MiniChip(text: String, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GlassBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier        = Modifier.size(40.dp),
        shape           = CircleShape,
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        shadowElevation = 14.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Box(modifier = Modifier.clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.onBackground
    )
}