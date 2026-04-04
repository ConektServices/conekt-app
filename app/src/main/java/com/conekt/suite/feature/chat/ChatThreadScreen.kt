package com.conekt.suite.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ChatThreadScreen(
    conversationId:  String,
    otherUserId:     String,
    otherName:       String,
    otherAvatarUrl:  String?,
    onBack:          () -> Unit,
    onOpenProfile:   (String) -> Unit,
    vm: ChatThreadViewModel = viewModel()
) {
    val state   by vm.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.init(conversationId, otherUserId) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    var showAttachMenu by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.sendFile(context, it) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0E))
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                // Avatar — tapping opens profile
                Box(Modifier.size(40.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f)).clickable { onOpenProfile(otherUserId) }) {
                    if (otherAvatarUrl != null) {
                        AsyncImage(otherAvatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(otherName, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("🔒 End-to-end encrypted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen.copy(alpha = 0.80f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Call, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { onOpenProfile(otherUserId) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Messages list ─────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            } else {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var lastDate = ""
                    state.messages.forEach { message ->
                        val date = formatDate(message.createdAt)
                        if (date != lastDate) {
                            lastDate = date
                            item(key = "date_$date") { DateSeparator(date) }
                        }
                        item(key = message.id) {
                            MessageBubble(message = message, onLongPress = { vm.deleteMessage(message.id) })
                        }
                    }
                    if (state.messages.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.40f), modifier = Modifier.size(32.dp))
                                    Text("Messages are end-to-end encrypted", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.35f), textAlign = TextAlign.Center)
                                    Text("Say hello 👋", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.50f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFF121218),
                border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column {
                    Row(
                        modifier          = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Attach button
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { showAttachMenu = !showAttachMenu },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (showAttachMenu) Icons.Rounded.Close else Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        // Text input
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.07f)) {
                            BasicTextField(
                                value         = state.draft,
                                onValueChange = vm::onDraftChange,
                                singleLine    = false,
                                maxLines      = 5,
                                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                modifier      = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                decorationBox = { inner ->
                                    if (state.draft.isEmpty()) Text("Message…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.35f))
                                    inner()
                                }
                            )
                        }

                        // Send button
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (state.draft.isNotBlank()) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))))
                                .clickable(enabled = state.draft.isNotBlank() && !state.isSending) { vm.sendText() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                    }

                    // Attach menu
                    AnimatedVisibility(showAttachMenu) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AttachChip(Icons.Rounded.Image, "Photo", InfoBlue) { fileLauncher.launch("image/*"); showAttachMenu = false }
                            AttachChip(Icons.Rounded.AttachFile, "File", Color(0xFF8B5CF6)) { fileLauncher.launch("*/*"); showAttachMenu = false }
                            AttachChip(Icons.Rounded.Headphones, "Music", BrandEnd) { /* open music picker */ showAttachMenu = false }
                            AttachChip(Icons.Rounded.Mic, "Audio", SuccessGreen) { fileLauncher.launch("audio/*"); showAttachMenu = false }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageItem, onLongPress: () -> Unit) {
    val isMe = message.isMe

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier            = Modifier.widthIn(max = 280.dp)
        ) {
            when (message.messageType) {
                "image" -> ImageBubble(message, isMe, onLongPress)
                "music" -> MusicBubble(message, isMe, onLongPress)
                "file"  -> FileBubble(message, isMe, onLongPress)
                "audio" -> AudioBubble(message, isMe, onLongPress)
                else    -> TextBubble(message, isMe, onLongPress)
            }
            Text(
                formatTime(message.createdAt),
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TextBubble(message: MessageItem, isMe: Boolean, onLongPress: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(
            topStart     = 20.dp, topEnd     = 20.dp,
            bottomStart  = if (isMe) 20.dp else 4.dp,
            bottomEnd    = if (isMe) 4.dp else 20.dp
        ),
        color = if (isMe) BrandEnd.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.10f)
    ) {
        Text(
            text     = if (message.isDeleted) "This message was deleted" else (message.body ?: ""),
            style    = MaterialTheme.typography.bodyMedium.copy(color = if (message.isDeleted) Color.White.copy(alpha = 0.40f) else Color.White),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).combinedClickable(onLongClick = onLongPress) {}
        )
    }
}

@Composable
private fun ImageBubble(message: MessageItem, isMe: Boolean, onLongPress: () -> Unit) {
    Box(
        modifier = Modifier.size(200.dp, 160.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onLongClick = onLongPress) {}
    ) {
        message.fileUrl?.let { url ->
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun MusicBubble(message: MessageItem, isMe: Boolean, onLongPress: () -> Unit) {
    val json = runCatching { Json.parseToJsonElement(message.body ?: "{}").jsonObject }.getOrNull()
    val title    = json?.get("title")?.jsonPrimitive?.content ?: "Unknown"
    val artist   = json?.get("artist")?.jsonPrimitive?.content ?: ""
    val coverUrl = json?.get("coverUrl")?.jsonPrimitive?.content?.ifBlank { null }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMe) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isMe) BrandEnd.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.combinedClickable(onLongClick = onLongPress) {}
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(BrandEnd.copy(alpha = 0.20f))) {
                coverUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd, modifier = Modifier.align(Alignment.Center).size(22.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
                Surface(shape = RoundedCornerShape(8.dp), color = BrandEnd.copy(alpha = 0.16f), modifier = Modifier.padding(top = 4.dp)) {
                    Text("🎵 Shared track", style = MaterialTheme.typography.labelSmall, color = BrandEnd, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun FileBubble(message: MessageItem, isMe: Boolean, onLongPress: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.combinedClickable(onLongClick = onLongPress) {}
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(InfoBlue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AttachFile, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(message.fileName ?: "File", color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tap to open", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.40f))
            }
        }
    }
}

@Composable
private fun AudioBubble(message: MessageItem, isMe: Boolean, onLongPress: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
        modifier = Modifier.combinedClickable(onLongClick = onLongPress) {}
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PlayArrow, null, tint = if (isMe) BrandEnd else Color.White, modifier = Modifier.size(24.dp))
            Box(Modifier.width(120.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.20f)))
            Text("0:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f))
        }
    }
}

@Composable
private fun AttachChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)).border(1.dp, accent.copy(alpha = 0.28f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.60f))
    }
}

@Composable
private fun DateSeparator(date: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.07f)) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

private fun formatTime(iso: String): String {
    return try {
        val inst = Instant.parse(iso)
        inst.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) { "" }
}

private fun formatDate(iso: String): String {
    return try {
        val inst = Instant.parse(iso)
        val zdt  = inst.atZone(ZoneId.systemDefault())
        val now  = java.time.ZonedDateTime.now()
        when {
            zdt.toLocalDate() == now.toLocalDate() -> "Today"
            zdt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
            else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) { "" }
}

@Composable
private fun BasicTextField(
    value: String, onValueChange: (String) -> Unit,
    singleLine: Boolean, maxLines: Int = Int.MAX_VALUE,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = singleLine,
        maxLines      = maxLines,
        textStyle     = textStyle,
        modifier      = modifier,
        keyboardOptions = keyboardOptions,
        decorationBox = decorationBox
    )
}
