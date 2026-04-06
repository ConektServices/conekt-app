package com.conekt.suite.feature.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import com.conekt.suite.data.model.PostRecord
import com.conekt.suite.ui.theme.*

private enum class UPTab { POSTS, ABOUT }

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onStartChat: (convId: String, otherId: String, name: String, avatar: String) -> Unit,
    vm: UserProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(UPTab.POSTS) }

    LaunchedEffect(userId) { vm.load(userId) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080C))) {

        when {
            // ── Loading ───────────────────────────────────────────────────────
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                        Text("Loading profile…", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.40f))
                    }
                }
                BackBtn(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp), onBack)
            }

            // ── Error ─────────────────────────────────────────────────────────
            state.profile == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PersonOff, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(32.dp))
                        }
                        Text("Profile not found", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(state.error ?: "This profile may be private or may not exist.", color = Color.White.copy(alpha = 0.38f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
                BackBtn(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp), onBack)
            }

            // ── Loaded ────────────────────────────────────────────────────────
            else -> {
                val p    = state.profile!!
                val name = p.displayName ?: p.username
                val posts = state.posts

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // ── Banner ────────────────────────────────────────────────
                    item(key = "banner") {
                        Box(Modifier.fillMaxWidth().height(240.dp)) {
                            if (p.bannerUrl?.isNotBlank() == true) {
                                AsyncImage(p.bannerUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                            } else {
                                // Gradient banner with ambient orbs
                                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A0A05), Color(0xFF200D08), Color(0xFF0D0614)))))
                                Box(Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
                                    .background(Brush.radialGradient(listOf(BrandEnd.copy(alpha = 0.26f), Color.Transparent))))
                                Box(Modifier.size(150.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp)
                                    .background(Brush.radialGradient(listOf(BrandStart.copy(alpha = 0.18f), Color.Transparent))))
                            }
                            // Gradient into background color at bottom
                            Box(Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF07080C)))))
                        }
                    }

                    // ── Avatar + action buttons ───────────────────────────────
                    item(key = "avatar_actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-44).dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Avatar with gradient ring
                            Box(
                                Modifier.size(92.dp)
                                    .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)), CircleShape)
                                    .padding(3.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .background(Color(0xFF07080C)).padding(2.dp).clip(CircleShape)
                                ) {
                                    p.avatarUrl?.ifBlank { null }?.let {
                                        AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } ?: Box(
                                        Modifier.fillMaxSize()
                                            .background(Brush.linearGradient(listOf(BrandStart.copy(0.30f), BrandEnd.copy(0.22f)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Follow / Unfollow button
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .then(
                                            if (state.isFollowing) {
                                                Modifier.background(Color.White.copy(alpha = 0.08f))
                                            } else {
                                                Modifier.background(ConektGradient.brandHorizontal)
                                            }
                                        )
                                        .border(
                                            if (state.isFollowing) 1.dp else 0.dp,
                                            Color.White.copy(alpha = 0.14f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { vm.toggleFollow(userId) }
                                        .padding(horizontal = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (state.isFollowing) "Following" else "Follow",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isFollowing) Color.White.copy(alpha = 0.65f) else Color.White
                                    )
                                }

                                // Message button
                                Box(
                                    modifier = Modifier.height(40.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                        .clickable(enabled = !state.isDmLoading) {
                                            vm.startDm(userId) { convId ->
                                                onStartChat(convId, userId, name, p.avatarUrl ?: "")
                                            }
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isDmLoading) {
                                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Icon(Icons.Rounded.ChatBubble, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Text("Message", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Name + bio ────────────────────────────────────────────
                    item(key = "identity") {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-32).dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                if (p.isVerified) Icon(Icons.Rounded.Verified, null, tint = BrandEnd, modifier = Modifier.size(20.dp))
                            }
                            Text("@${p.username}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.42f), modifier = Modifier.padding(top = 2.dp))
                            p.bio?.ifBlank { null }?.let { bio ->
                                Text(bio, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f), modifier = Modifier.padding(top = 12.dp), lineHeight = 22.sp)
                            }

                            Spacer(Modifier.height(16.dp))

                            // ── Stats row — follower + following counts ────────
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatColumn(value = p.followerCount.toString(), label = "Followers")
                                    Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.10f)))
                                    StatColumn(value = p.followingCount.toString(), label = "Following")
                                    Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.10f)))
                                    StatColumn(value = posts.size.toString(), label = "Posts")
                                }
                            }
                        }
                    }

                    // ── Full-width "Send a Message" CTA ───────────────────────
                    item(key = "cta") {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-18).dp)) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(ConektGradient.brandHorizontal)
                                    .clickable(enabled = !state.isDmLoading) {
                                        vm.startDm(userId) { convId ->
                                            onStartChat(convId, userId, name, p.avatarUrl ?: "")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isDmLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Send a Message", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // ── Tab bar ───────────────────────────────────────────────
                    item(key = "tabs") {
                        Surface(
                            modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-8).dp),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                UPTab.entries.forEach { tab ->
                                    val sel = tab == selectedTab
                                    Box(
                                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                            .background(if (sel) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable { selectedTab = tab }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            when (tab) {
                                                UPTab.POSTS -> "Posts (${posts.size})"
                                                UPTab.ABOUT -> "About"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Tab content ───────────────────────────────────────────
                    when (selectedTab) {
                        UPTab.POSTS -> {
                            if (posts.isEmpty()) {
                                item(key = "posts_empty") {
                                    EmptyPostsState(name = name.substringBefore(" "))
                                }
                            } else {
                                // Posts with media shown as 3-column image grid
                                val mediaPosts = posts.filter { it.mediaUrls.any { u -> u.isNotBlank() } }
                                val textPosts  = posts.filter { it.mediaUrls.none { u -> u.isNotBlank() } }

                                if (mediaPosts.isNotEmpty()) {
                                    item(key = "media_grid_header") {
                                        Text("Photos & Media", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.50f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                                    }
                                    item(key = "media_grid") {
                                        PostsMediaGrid(mediaPosts = mediaPosts)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }

                                if (textPosts.isNotEmpty()) {
                                    item(key = "text_posts_header") {
                                        Text("Posts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.50f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                                    }
                                    items(textPosts, key = { "tp_${it.id}" }) { post ->
                                        TextPostCard(post = post, name = name, avatarUrl = p.avatarUrl)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                        UPTab.ABOUT -> {
                            item(key = "about") {
                                AboutSection(profile = p)
                            }
                        }
                    }
                }

                // Back button overlaid over the banner
                BackBtn(
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
                    onBack = onBack,
                    dark = true
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Posts grid (media posts shown as photo grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PostsMediaGrid(mediaPosts: List<PostRecord>) {
    val images = mediaPosts.flatMap { post ->
        post.mediaUrls.filter { it.isNotBlank() }
    }

    // Simple 3-column grid
    val rows = images.chunked(3)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        rows.forEach { rowImages ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                rowImages.forEach { imageUrl ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        AsyncImage(imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                // Fill remaining cells in last row
                repeat(3 - rowImages.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TextPostCard(post: PostRecord, name: String, avatarUrl: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.16f))) {
                    avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(name.first().uppercaseChar().toString(), color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                }
                Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!post.body.isNullOrBlank()) {
                Text(post.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp), lineHeight = 22.sp)
            }
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Text("${post.likeCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Text("${post.commentCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(profile: OtherUserProfile) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!profile.bio.isNullOrBlank()) {
            AboutCard(icon = Icons.Rounded.Person, label = "Bio") {
                Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
            }
        }
        AboutCard(icon = Icons.Rounded.AlternateEmail, label = "Username") {
            Text("@${profile.username}", style = MaterialTheme.typography.bodyMedium, color = BrandEnd)
        }
        AboutCard(icon = Icons.Rounded.Group, label = "Network") {
            Text("${profile.followerCount} followers · ${profile.followingCount} following", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (profile.isVerified) {
            AboutCard(icon = Icons.Rounded.Verified, label = "Status") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verified account", style = MaterialTheme.typography.bodyMedium, color = BrandEnd)
                    Icon(Icons.Rounded.Verified, null, tint = BrandEnd, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty states + helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyPostsState(name: String) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.40f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.GridView, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(40.dp))
            Text("No posts yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text("When $name posts, they'll show here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.42f))
    }
}

@Composable
private fun BackBtn(modifier: Modifier, onBack: () -> Unit, dark: Boolean = false) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape)
            .background(if (dark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun relativeTime(iso: String): String = try {
    val d = java.time.Duration.between(java.time.Instant.parse(iso), java.time.Instant.now())
    when {
        d.toMinutes() < 1 -> "just now"
        d.toHours() < 1 -> "${d.toMinutes()}m"
        d.toDays() < 1 -> "${d.toHours()}h"
        d.toDays() < 7 -> "${d.toDays()}d"
        else -> java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
    }
} catch (_: Exception) { "" }