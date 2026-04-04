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

@Composable
fun UserProfileScreen(
    userId:         String,
    onBack:         () -> Unit,
    onStartChat:    (convId: String, otherId: String, name: String, avatar: String?) -> Unit,
    vm: UserProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val threadVm: ChatThreadViewModel = viewModel()

    LaunchedEffect(userId) { vm.load(userId) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0E))) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandEnd, strokeWidth = 2.dp)
            }
            state.profile == null -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PersonOff, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(48.dp))
                    Text("Profile not found", color = Color.White.copy(alpha = 0.50f), modifier = Modifier.padding(top = 12.dp))
                }
            }
            else -> {
                val profile = state.profile!!
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Banner
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            // Banner bg
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.30f), BrandEnd.copy(alpha = 0.20f), Color(0xFF0A0A0E)))))
                            profile.bannerUrl?.ifBlank { null }?.let {
                                AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                            }
                            // Back button
                            Box(
                                Modifier.padding(top = 56.dp, start = 16.dp).size(38.dp).clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.40f)).clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Avatar + name row
                    item {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            // Avatar
                            Box(
                                modifier = Modifier.offset(y = (-38).dp)
                                    .size(80.dp).clip(CircleShape)
                                    .border(3.dp, Color(0xFF0A0A0E), CircleShape)
                                    .background(BrandEnd.copy(alpha = 0.20f))
                            ) {
                                profile.avatarUrl?.ifBlank { null }?.let {
                                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        (profile.displayName ?: profile.username).first().uppercaseChar().toString(),
                                        style = MaterialTheme.typography.headlineSmall, color = BrandEnd, fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Action buttons
                            Row(
                                Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Message button
                                Surface(
                                    shape  = RoundedCornerShape(14.dp),
                                    color  = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                ) {
                                    Box(
                                        Modifier.clickable {
                                            // start or open a DM
                                        }.padding(horizontal = 14.dp, vertical = 9.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.ChatBubble, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Message", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // Follow button
                                val following = state.isFollowing
                                Surface(
                                    shape    = RoundedCornerShape(14.dp),
                                    color    = if (following) Color.Transparent else BrandEnd,
                                    border   = if (following) BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)) else null
                                ) {
                                    Box(
                                        Modifier.clickable { vm.toggleFollow(userId) }.padding(horizontal = 18.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            if (following) "Following" else "Follow",
                                            style      = MaterialTheme.typography.labelMedium,
                                            color      = if (following) Color.White.copy(alpha = 0.70f) else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Name + bio
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-28).dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(profile.displayName ?: profile.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                if (profile.isVerified) Icon(Icons.Rounded.Verified, null, tint = BrandEnd, modifier = Modifier.size(18.dp))
                            }
                            Text("@${profile.username}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 2.dp))
                            profile.bio?.ifBlank { null }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.80f), modifier = Modifier.padding(top = 10.dp))
                            }
                            // Stats row
                            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                StatItem("${profile.followerCount}", "Followers")
                                StatItem("${profile.followingCount}", "Following")
                            }
                        }
                    }

                    // Posts placeholder (you'd pull from PulseRepository)
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(20.dp).height(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(32.dp))
                                Text("No posts yet", color = Color.White.copy(alpha = 0.35f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
    }
}
