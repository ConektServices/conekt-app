package com.conekt.suite.feature.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    userId:      String,
    onBack:      () -> Unit,
    onStartChat: (convId: String, otherId: String, name: String, avatar: String?) -> Unit,
    vm: UserProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) { vm.load(userId) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090D))) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandEnd, strokeWidth = 2.dp)
            }

            state.profile == null -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PersonOff, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(48.dp))
                    Text("Profile not found", color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 12.dp))
                }
                Box(Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 16.dp).size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            else -> {
                val profile = state.profile!!
                val name    = profile.displayName ?: profile.username

                LazyColumn(Modifier.fillMaxSize()) {

                    // ── Banner ────────────────────────────────────────────────
                    item(key = "banner") {
                        Box(Modifier.fillMaxWidth().height(220.dp)) {
                            // Background
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.28f), BrandEnd.copy(alpha = 0.18f), Color(0xFF08090D)))))
                            profile.bannerUrl?.ifBlank { null }?.let { url ->
                                AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                            }
                            // Back btn
                            Box(
                                Modifier.statusBarsPadding().padding(start = 16.dp, top = 10.dp)
                                    .size(38.dp).clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.38f))
                                    .clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // ── Avatar + actions ──────────────────────────────────────
                    item(key = "avatar_actions") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-44).dp), verticalAlignment = Alignment.Bottom) {
                            // Avatar
                            Box(
                                Modifier.size(88.dp).clip(CircleShape)
                                    .border(3.dp, Color(0xFF08090D), CircleShape)
                                    .background(BrandEnd.copy(alpha = 0.20f))
                            ) {
                                profile.avatarUrl?.ifBlank { null }?.let {
                                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineSmall, color = BrandEnd, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Action buttons
                            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Message
                                Surface(
                                    shape  = RoundedCornerShape(14.dp),
                                    color  = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                ) {
                                    Box(
                                        Modifier.clickable {
                                            scope.launch {
                                                val convId = vm.getOrCreateDm(userId)
                                                if (convId.isNotBlank()) {
                                                    onStartChat(convId, userId, name, profile.avatarUrl)
                                                }
                                            }
                                        }.padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.ChatBubble, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Text("Message", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // Follow / Unfollow
                                val following = state.isFollowing
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (following) Color.Transparent else BrandEnd,
                                    border = if (following) BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)) else null
                                ) {
                                    Box(Modifier.clickable { vm.toggleFollow(userId) }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                                        Text(
                                            if (following) "Following" else "Follow",
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (following) Color.White.copy(alpha = 0.65f) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Bio ───────────────────────────────────────────────────
                    item(key = "bio") {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-32).dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                if (profile.isVerified) {
                                    Icon(Icons.Rounded.Verified, null, tint = BrandEnd, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text("@${profile.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.40f), modifier = Modifier.padding(top = 2.dp))

                            profile.bio?.ifBlank { null }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f), modifier = Modifier.padding(top = 12.dp))
                            }

                            // Stats
                            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                ProfileStat("${profile.followerCount}", "Followers")
                                ProfileStat("${profile.followingCount}", "Following")
                            }
                        }
                    }

                    // ── Divider ───────────────────────────────────────────────
                    item(key = "div") {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-16).dp))
                    }

                    // ── Posts grid placeholder ────────────────────────────────
                    item(key = "posts_empty") {
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp).height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White.copy(alpha = 0.20f), modifier = Modifier.size(34.dp))
                                Text("No posts yet", color = Color.White.copy(alpha = 0.30f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.40f))
    }
}