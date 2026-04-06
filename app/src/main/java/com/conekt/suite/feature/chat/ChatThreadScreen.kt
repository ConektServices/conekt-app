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

// ── Common emoji set for picker ───────────────────────────────────────────────
private val EMOJIS = listOf(
    "😀","😂","🥰","😍","🤩","😎","🥺","😭","🤔","😤",
    "🎉","🔥","💯","❤️","🧡","💛","💚","💙","💜","🖤",
    "👍","👎","👏","🙌","🤝","✌️","🤞","🫶","💪","🙏",
    "🍕","🍔","🍟","🌮","🍣","🍜","🍰","🎂","☕","🧃",
    "🚀","✈️","🎵","🎮","⚽","🏀","🎯","🏆","💎","👑"
)

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

    // Init once
    LaunchedEffect(conversationId) {
        vm.init(conversationId, otherUserId)
    }

    // Scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.sendFile(context, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090F))
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            ChatTopBar(
                name      = otherName,
                avatarUrl = otherAvatarUrl,
                onBack    = onBack,
                onProfile = { onOpenProfile(otherUserId) }
            )

            // ── Messages list ─────────────────────────────────────────────────
            Box(Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color       = BrandEnd,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(28.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (state.messages.isEmpty()) {
                            item(key = "empty_state") {
                                Box(
                                    Modifier
                                        .fillParentMaxWidth()
                                        .padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(64.dp).clip(CircleShape)
                                                .background(SuccessGreen.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.Lock,
                                                null,
                                                tint     = SuccessGreen.copy(alpha = 0.60f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            "Messages are end-to-end encrypted",
                                            style   = MaterialTheme.typography.bodySmall,
                                            color   = Color.White.copy(alpha = 0.35f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Say hi to ${otherName.substringBefore(" ")} 👋",
                                            style   = MaterialTheme.typography.bodyMedium,
                                            color   = Color.White.copy(alpha = 0.50f)
                                        )
                                    }
                                }
                            }
                        } else {
                            var prevDate = ""
                            state.messages.forEach { msg ->
                                val date = msgDate(msg.createdAt)
                                if (date != prevDate) {
                                    prevDate = date
                                    item(key = "divider_$date") { DateLabel(date) }
                                }
                                item(key = msg.id) {
                                    Bubble(
                                        msg      = msg,
                                        onDelete = { vm.deleteMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Emoji panel ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.showEmoji,
                enter   = expandVertically(expandFrom = Alignment.Bottom),
                exit    = shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                EmojiPicker { emoji -> vm.appendEmoji(emoji) }
            }

            // ── Attach menu ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.showAttach,
                enter   = expandVertically(expandFrom = Alignment.Bottom),
                exit    = shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                AttachMenu(
                    onPhoto = { fileLauncher.launch("image/*") },
                    onFile  = { fileLauncher.launch("*/*") },
                    onAudio = { fileLauncher.launch("audio/*") }
                )
            }

            // ── Input bar ─────────────────────────────────────────────────────
            InputBar(
                draft      = state.draft,
                isSending  = state.isSending,
                showEmoji  = state.showEmoji,
                showAttach = state.showAttach,
                onDraft    = vm::onDraftChange,
                onEmoji    = vm::toggleEmoji,
                onAttach   = vm::toggleAttach,
                onSend     = vm::sendText
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    name:      String,
    avatarUrl: String?,
    onBack:    () -> Unit,
    onProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF0F0F16),
        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandEnd.copy(alpha = 0.20f))
                    .clickable { onProfile() }
            ) {
                avatarUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color      = BrandEnd,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.75f), modifier = Modifier.size(11.dp))
                    Text("End-to-end encrypted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen.copy(alpha = 0.65f))
                }
            }

            // Info / profile shortcut
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    draft:      String,
    isSending:  Boolean,
    showEmoji:  Boolean,
    showAttach: Boolean,
    onDraft:    (String) -> Unit,
    onEmoji:    () -> Unit,
    onAttach:   () -> Unit,
    onSend:     () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF0F0F16),
        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emoji toggle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (showEmoji) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f))
                    .clickable { onEmoji() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (showEmoji) "⌨️" else "😊", fontSize = 18.sp)
            }

            // Attach toggle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (showAttach) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f))
                    .clickable { onAttach() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (showAttach) Icons.Rounded.Close else Icons.Rounded.Add,
                    null,
                    tint     = if (showAttach) BrandEnd else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text field
            Surface(
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(22.dp),
                color    = Color.White.copy(alpha = 0.07f)
            ) {
                BasicTextField(
                    value           = draft,
                    onValueChange   = onDraft,
                    singleLine      = false,
                    maxLines        = 5,
                    textStyle       = TextStyle(color = Color.White, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    decorationBox   = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                "Message…",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = Color.White.copy(alpha = 0.28f)
                            )
                        }
                        inner()
                    }
                )
            }

            // Send button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (draft.isNotBlank()) ConektGradient.brandHorizontal
                        else Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f))
                        )
                    )
                    .clickable(enabled = draft.isNotBlank() && !isSending) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.Send,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emoji picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmojiPicker(onPick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF14141E)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Emojis",
                style    = MaterialTheme.typography.labelMedium,
                color    = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val rows = EMOJIS.chunked(10)
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .padding(3.dp)
                                .clickable { onPick(emoji) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Attach menu
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttachMenu(
    onPhoto: () -> Unit,
    onFile:  () -> Unit,
    onAudio: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF14141E)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AttachOption(Icons.Rounded.Image,       "Photo",  InfoBlue)         { onPhoto() }
            AttachOption(Icons.Rounded.AttachFile,  "File",   Color(0xFF8B5CF6)) { onFile() }
            AttachOption(Icons.Rounded.Mic,         "Audio",  SuccessGreen)     { onAudio() }
            AttachOption(Icons.Rounded.Headphones,  "Music",  BrandEnd)         { /* music picker handled separately */ }
        }
    }
}

@Composable
private fun AttachOption(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    accent:  Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.28f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubble dispatcher
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Bubble(msg: MessageItem, onDelete: () -> Unit) {
    val isMe = msg.isMe

    if (msg.isDeleted) {
        DeletedBubble(isMe)
        return
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        Column(
            modifier            = Modifier.widthIn(max = 285.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            when (msg.type) {
                MsgType.IMAGE -> ImageBubble(msg, isMe, onDelete)
                MsgType.MUSIC -> MusicBubble(msg, isMe, onDelete)
                MsgType.FILE  -> FileBubble(msg, isMe, onDelete)
                MsgType.AUDIO -> AudioBubble(msg, isMe, onDelete)
                MsgType.EMOJI -> EmojiBubble(msg, onDelete)
                MsgType.TEXT  -> TextBubble(msg, isMe, onDelete)
            }
            Spacer(Modifier.height(1.dp))
            Row(
                modifier              = Modifier.padding(horizontal = 4.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(msgTime(msg.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.28f))
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.DoneAll, null, tint = BrandEnd.copy(alpha = 0.60f), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

// ── Text bubble ───────────────────────────────────────────────────────────────

@Composable
private fun TextBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(
            topStart    = 20.dp,
            topEnd      = 20.dp,
            bottomStart = if (isMe) 20.dp else 4.dp,
            bottomEnd   = if (isMe) 4.dp  else 20.dp
        ),
        color = if (isMe) BrandEnd else Color.White.copy(alpha = 0.10f)
    ) {
        Text(
            text     = msg.body ?: "",
            style    = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .combinedClickable(onLongClick = onDelete) {}
        )
    }
}

// ── Emoji bubble ──────────────────────────────────────────────────────────────

@Composable
private fun EmojiBubble(msg: MessageItem, onDelete: () -> Unit) {
    Text(
        text     = msg.body ?: "",
        fontSize = 40.sp,
        modifier = Modifier
            .padding(4.dp)
            .combinedClickable(onLongClick = onDelete) {}
    )
}

// ── Image bubble ──────────────────────────────────────────────────────────────

@Composable
private fun ImageBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 175.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onLongClick = onDelete) {}
    ) {
        msg.fileUrl?.let {
            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Slight gradient overlay for polish
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)))))
    }
}

// ── Music bubble ──────────────────────────────────────────────────────────────

@Composable
private fun MusicBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isMe) BrandEnd.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier              = Modifier
                .padding(12.dp)
                .combinedClickable(onLongClick = onDelete) {},
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandEnd.copy(alpha = 0.20f))
            ) {
                msg.musicCover?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd, modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(msg.musicTitle ?: "Track", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(msg.musicArtist ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = BrandEnd.copy(alpha = 0.16f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = BrandEnd, modifier = Modifier.size(14.dp))
                        Text("Play track", style = MaterialTheme.typography.labelSmall, color = BrandEnd)
                    }
                }
            }
        }
    }
}

// ── File bubble ───────────────────────────────────────────────────────────────

@Composable
private fun FileBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier              = Modifier
                .padding(12.dp)
                .combinedClickable(onLongClick = onDelete) {},
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(InfoBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.InsertDriveFile, null, tint = InfoBlue, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(msg.fileName ?: "File", color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tap to open", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.40f))
            }
            Icon(Icons.Rounded.Download, null, tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Audio bubble ──────────────────────────────────────────────────────────────

@Composable
private fun AudioBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier              = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .combinedClickable(onLongClick = onDelete) {},
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (isMe) BrandEnd else Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            // Fake waveform
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                val heights = listOf(8, 14, 10, 18, 12, 8, 16, 10, 14, 8, 12, 16, 10)
                heights.forEach { h ->
                    Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.55f)))
                }
            }
            Text("0:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f))
        }
    }
}

// ── Deleted bubble ────────────────────────────────────────────────────────────

@Composable
private fun DeletedBubble(isMe: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Block, null, tint = Color.White.copy(alpha = 0.28f), modifier = Modifier.size(14.dp))
                Text("Message deleted", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.30f))
            }
        }
    }
}

// ── Date label ────────────────────────────────────────────────────────────────

@Composable
private fun DateLabel(label: String) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.07f)) {
            Text(
                label,
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun msgTime(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

private fun msgDate(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    when {
        zdt.toLocalDate() == now.toLocalDate()              -> "Today"
        zdt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}.getOrDefault("")
