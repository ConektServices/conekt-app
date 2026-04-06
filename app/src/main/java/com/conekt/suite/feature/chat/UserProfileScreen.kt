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
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*

@Composable
fun UserProfileScreen(
    userId:      String,
    onBack:      () -> Unit,
    onStartChat: (convId: String, otherId: String, name: String, avatar: String) -> Unit,
    vm: UserProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(userId) { vm.load(userId) }

    Box(Modifier.fillMaxSize().background(Color(0xFF09090F))) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandEnd, strokeWidth = 2.dp)
                BackButton(Modifier.align(Alignment.TopStart), onBack)
            }
            state.profile == null -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PersonOff, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(48.dp))
                    Text("Profile not found", color = Color.White.copy(alpha = 0.38f), modifier = Modifier.padding(top = 10.dp))
                }
                BackButton(Modifier.align(Alignment.TopStart), onBack)
            }
            else -> {
                val p    = state.profile!!
                val name = p.displayName ?: p.username

                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {

                    // ── Banner ────────────────────────────────────────────────
                    item(key = "banner") {
                        Box(Modifier.fillMaxWidth().height(220.dp)) {
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.30f), BrandEnd.copy(alpha = 0.22f), Color(0xFF09090F)))))
                            p.bannerUrl?.ifBlank { null }?.let { url ->
                                AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
                            }
                        }
                    }

                    // ── Avatar + action buttons ───────────────────────────────
                    item(key = "avatar_actions") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-44).dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Avatar
                            Box(
                                Modifier.size(90.dp).clip(CircleShape)
                                    .border(3.dp, Color(0xFF09090F), CircleShape)
                                    .background(BrandEnd.copy(alpha = 0.20f))
                            ) {
                                p.avatarUrl?.ifBlank { null }?.let {
                                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineSmall, color = BrandEnd, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Message
                                Surface(
                                    shape  = RoundedCornerShape(14.dp),
                                    color  = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable(enabled = !state.isDmLoading) {
                                                vm.startDm(userId) { convId ->
                                                    onStartChat(convId, userId, name, p.avatarUrl ?: "")
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 9.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (state.isDmLoading) {
                                            CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Rounded.ChatBubble, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                        Text("Message", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Follow / Unfollow
                                val following = state.isFollowing
                                Surface(
                                    shape  = RoundedCornerShape(14.dp),
                                    color  = if (following) Color.Transparent else BrandEnd,
                                    border = if (following) BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)) else null
                                ) {
                                    Box(
                                        Modifier.clickable { vm.toggleFollow(userId) }.padding(horizontal = 20.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            if (following) "Following" else "Follow",
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (following) Color.White.copy(alpha = 0.60f) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Name + bio ────────────────────────────────────────────
                    item(key = "bio") {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-26).dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                if (p.isVerified) Icon(Icons.Rounded.Verified, null, tint = BrandEnd, modifier = Modifier.size(18.dp))
                            }
                            Text("@${p.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.38f), modifier = Modifier.padding(top = 2.dp))
                            p.bio?.ifBlank { null }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 12.dp))
                            }
                            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                StatCol(p.followerCount.toString(),  "Followers")
                                StatCol(p.followingCount.toString(), "Following")
                            }
                        }
                    }

                    item(key = "divider") {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-10).dp))
                    }

                    // ── Posts grid placeholder ────────────────────────────────
                    item(key = "posts") {
                        Box(
                            Modifier.fillMaxWidth().padding(20.dp).height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(32.dp))
                                Text("No posts yet", color = Color.White.copy(alpha = 0.28f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Back button overlaid on banner
                BackButton(
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 10.dp),
                    onBack   = onBack,
                    dark     = true
                )
            }
        }
    }
}

@Composable
private fun BackButton(modifier: Modifier, onBack: () -> Unit, dark: Boolean = false) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.07f))
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.38f))
    }
}