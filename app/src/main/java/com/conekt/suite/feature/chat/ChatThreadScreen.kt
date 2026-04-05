package com.conekt.suite.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatThreadScreen(
    conversationId: String,
    otherUserId:    String,
    otherName:      String,
    otherAvatarUrl: String?,
    onBack:         () -> Unit,
    onOpenProfile:  (String) -> Unit,
    vm: ChatThreadViewModel = viewModel()
) {
    val state     by vm.state.collectAsState()
    val context   = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.init(conversationId, otherUserId) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    var showAttach by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.sendFile(context, it) }
        showAttach = false
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF08090D))) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.18f)).clickable { onOpenProfile(otherUserId) }
                ) {
                    otherAvatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold)
                        }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(otherName, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.80f), modifier = Modifier.size(11.dp))
                        Text("End-to-end encrypted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen.copy(alpha = 0.70f))
                    }
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { onOpenProfile(otherUserId) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                }
            }

            // ── Messages ──────────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            } else {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (state.messages.isEmpty()) {
                        item(key = "empty") {
                            Box(Modifier.fillParentMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.35f), modifier = Modifier.size(32.dp))
                                    Text("Messages are end-to-end encrypted", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.30f), textAlign = TextAlign.Center)
                                    Text("Say hello 👋", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.45f))
                                }
                            }
                        }
                    } else {
                        var lastDate = ""
                        state.messages.forEach { msg ->
                            val d = threadFormatDate(msg.createdAt)
                            if (d != lastDate) {
                                lastDate = d
                                item(key = "date_$d") { DateDivider(d) }
                            }
                            item(key = msg.id) {
                                MsgBubble(msg = msg, onDelete = { vm.deleteMessage(msg.id) })
                            }
                        }
                    }
                }
            }

            // ── Input ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF0F1015))
                    .navigationBarsPadding()
            ) {
                // Attach chips
                AnimatedVisibility(visible = showAttach, enter = expandVertically(), exit = shrinkVertically()) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AttachBtn(Icons.Rounded.Image,       "Photo",  InfoBlue)         { fileLauncher.launch("image/*") }
                        AttachBtn(Icons.Rounded.AttachFile,  "File",   Color(0xFF8B5CF6)) { fileLauncher.launch("*/*") }
                        AttachBtn(Icons.Rounded.Headphones,  "Music",  BrandEnd)         { /* music picker */ showAttach = false }
                        AttachBtn(Icons.Rounded.Mic,         "Audio",  SuccessGreen)     { fileLauncher.launch("audio/*") }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attach toggle
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (showAttach) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f))
                            .clickable { showAttach = !showAttach },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (showAttach) Icons.Rounded.Close else Icons.Rounded.Add, null, tint = if (showAttach) BrandEnd else Color.White, modifier = Modifier.size(22.dp))
                    }

                    // Text field
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.07f)) {
                        BasicTextField(
                            value           = state.draft,
                            onValueChange   = vm::onDraftChange,
                            singleLine      = false,
                            maxLines        = 5,
                            textStyle       = TextStyle(color = Color.White, fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier        = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                            decorationBox   = { inner ->
                                if (state.draft.isEmpty()) Text("Message…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.30f))
                                inner()
                            }
                        )
                    }

                    // Send
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (state.draft.isNotBlank()) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f))))
                            .clickable(enabled = state.draft.isNotBlank() && !state.isSending) { vm.sendText() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MsgBubble(msg: MessageItem, onDelete: () -> Unit) {
    val isMe = msg.isMe
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier            = Modifier.widthIn(max = 280.dp)
        ) {
            when (msg.messageType) {
                "image" -> ImageBubble(msg, isMe, onDelete)
                "music" -> MusicMsgBubble(msg, isMe, onDelete)
                "audio" -> AudioBubble(msg, isMe, onDelete)
                "file"  -> FileBubble(msg, isMe, onDelete)
                else    -> TextBubble(msg, isMe, onDelete)
            }
            Text(
                threadFormatTime(msg.createdAt),
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.28f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TextBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isMe) 20.dp else 5.dp,
            bottomEnd   = if (isMe) 5.dp  else 20.dp
        ),
        color = if (isMe) BrandEnd else Color.White.copy(alpha = 0.10f)
    ) {
        Text(
            text     = if (msg.isDeleted) "This message was deleted" else (msg.body ?: ""),
            style    = MaterialTheme.typography.bodyMedium.copy(
                color = if (msg.isDeleted) Color.White.copy(alpha = 0.38f) else Color.White
            ),
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .combinedClickable(onLongClick = onDelete) {}
        )
    }
}

@Composable
private fun ImageBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Box(
        Modifier.size(210.dp, 165.dp).clip(RoundedCornerShape(16.dp))
            .combinedClickable(onLongClick = onDelete) {}
    ) {
        msg.fileUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun MusicMsgBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    val json     = runCatching { Json.parseToJsonElement(msg.body ?: "{}").jsonObject }.getOrNull()
    val title    = json?.get("title")?.jsonPrimitive?.content ?: "Unknown"
    val artist   = json?.get("artist")?.jsonPrimitive?.content ?: ""
    val coverUrl = json?.get("coverUrl")?.jsonPrimitive?.content?.ifBlank { null }

    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isMe) BrandEnd.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.combinedClickable(onLongClick = onDelete) {}
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(BrandEnd.copy(alpha = 0.20f))) {
                coverUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd, modifier = Modifier.align(Alignment.Center).size(22.dp))
            }
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.52f))
                Surface(shape = RoundedCornerShape(8.dp), color = BrandEnd.copy(alpha = 0.14f), modifier = Modifier.padding(top = 4.dp)) {
                    Text("🎵 Shared track", style = MaterialTheme.typography.labelSmall, color = BrandEnd, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun FileBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        modifier = Modifier.combinedClickable(onLongClick = onDelete) {}
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(InfoBlue.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AttachFile, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(msg.fileName ?: "File", color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tap to open", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.38f))
            }
        }
    }
}

@Composable
private fun AudioBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = if (isMe) BrandEnd.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
        modifier = Modifier.combinedClickable(onLongClick = onDelete) {}
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.PlayArrow, null, tint = if (isMe) BrandEnd else Color.White, modifier = Modifier.size(24.dp))
            Box(Modifier.width(110.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.18f)))
            Text("0:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
        }
    }
}

// ── Attach button ─────────────────────────────────────────────────────────────

@Composable
private fun AttachBtn(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    accent:  Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(accent.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.25f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
    }
}

// ── Date divider ──────────────────────────────────────────────────────────────

@Composable
private fun DateDivider(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.06f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.40f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

// ── Time helpers ──────────────────────────────────────────────────────────────

private fun threadFormatTime(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

private fun threadFormatDate(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    when {
        zdt.toLocalDate() == now.toLocalDate()                 -> "Today"
        zdt.toLocalDate() == now.toLocalDate().minusDays(1)    -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}.getOrDefault("")