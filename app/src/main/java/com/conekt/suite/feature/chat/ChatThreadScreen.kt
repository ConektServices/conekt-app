package com.conekt.suite.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    otherUserId: String,
    otherName: String,
    otherAvatarUrl: String?,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    vm: ChatThreadViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // State for the slide-in profile panel
    var showProfilePanel by remember { mutableStateOf(false) }

    // Init conversation when screen opens
    LaunchedEffect(conversationId, otherUserId) {
        vm.init(conversationId, otherUserId)
    }

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.sendFile(context, it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF09090F))) {

        // ── Main chat UI ──────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back + avatar/name (tappable to open profile panel)
            ThreadTopBar(
                name = otherName,
                avatarUrl = otherAvatarUrl,
                onBack = onBack,
                onOpenProfilePanel = { showProfilePanel = true }
            )

            // Messages
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (state.messages.isEmpty()) {
                            item(key = "empty") {
                                EmptyThreadHint(otherName = otherName)
                            }
                        } else {
                            var prevDate = ""
                            state.messages.forEach { msg ->
                                val date = msgDate(msg.createdAt)
                                if (date != prevDate) {
                                    prevDate = date
                                    item(key = "date_$date") { DateDivider(date) }
                                }
                                item(key = msg.id) {
                                    MessageBubble(msg = msg, onDelete = { vm.deleteMessage(msg.id) })
                                }
                            }
                        }
                    }
                }
            }

            // Emoji picker panel
            AnimatedVisibility(
                visible = state.showEmoji,
                enter = expandVertically(expandFrom = Alignment.Bottom),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                EmojiPanel(onPick = { emoji -> vm.appendEmoji(emoji) })
            }

            // Attach panel
            AnimatedVisibility(
                visible = state.showAttach,
                enter = expandVertically(expandFrom = Alignment.Bottom),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                AttachPanel(
                    onPhoto = { fileLauncher.launch("image/*") },
                    onFile = { fileLauncher.launch("*/*") },
                    onAudio = { fileLauncher.launch("audio/*") }
                )
            }

            // Message input bar
            MessageInputBar(
                draft = state.draft,
                isSending = state.isSending,
                showEmoji = state.showEmoji,
                showAttach = state.showAttach,
                onDraftChange = vm::onDraftChange,
                onToggleEmoji = vm::toggleEmoji,
                onToggleAttach = vm::toggleAttach,
                onSend = vm::sendText
            )
        }

        // ── Profile side panel (slides in from the right) ─────────────────────
        // Backdrop
        if (showProfilePanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable { showProfilePanel = false }
            )
        }

        // Panel itself
        AnimatedVisibility(
            visible = showProfilePanel,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(220, easing = FastOutLinearInEasing)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ProfileSidePanel(
                name = otherName,
                avatarUrl = otherAvatarUrl,
                onClose = { showProfilePanel = false },
                onViewFullProfile = {
                    showProfilePanel = false
                    onOpenProfile(otherUserId)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThreadTopBar(
    name: String,
    avatarUrl: String?,
    onBack: () -> Unit,
    onOpenProfilePanel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F0F16),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(10.dp))

            // Avatar (tap to open profile panel)
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(BrandEnd.copy(alpha = 0.20f))
                    .clickable { onOpenProfilePanel() }
            ) {
                avatarUrl?.ifBlank { null }?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.width(10.dp))

            // Name + encryption label (tap to open profile panel)
            Column(
                modifier = Modifier.weight(1f).clickable { onOpenProfilePanel() }
            ) {
                Text(name, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.75f), modifier = Modifier.size(11.dp))
                    Text("Encrypted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen.copy(alpha = 0.65f))
                }
            }

            // Info button (opens profile panel)
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onOpenProfilePanel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile side panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSidePanel(
    name: String,
    avatarUrl: String?,
    onClose: () -> Unit,
    onViewFullProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(290.dp),
        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
        color = Color(0xFF111120),
        shadowElevation = 32.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient glow
            Box(Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
                .background(Brush.radialGradient(listOf(BrandEnd.copy(alpha = 0.20f), Color.Transparent))))

            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)
            ) {
                // Close button
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.height(24.dp))

                // Avatar with gradient ring
                Box(
                    Modifier.size(80.dp)
                        .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)), CircleShape)
                        .padding(3.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF111120)).padding(2.dp).clip(CircleShape)) {
                        avatarUrl?.ifBlank { null }?.let {
                            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } ?: Box(Modifier.fillMaxSize().background(BrandEnd.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            Text(name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen))
                    Text("Active now", style = MaterialTheme.typography.bodySmall, color = SuccessGreen.copy(alpha = 0.80f))
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                Spacer(Modifier.height(20.dp))

                // Info rows
                PanelInfoRow(icon = Icons.Rounded.ChatBubbleOutline, title = "Chat", subtitle = "Tap message to continue")
                Spacer(Modifier.height(10.dp))
                PanelInfoRow(icon = Icons.Rounded.Lock, title = "End-to-end encrypted", subtitle = "Messages are private & secure")

                Spacer(Modifier.weight(1f))

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .clickable { onViewFullProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                            Text("View Full Profile", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ConektGradient.brandHorizontal)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Back to Chat", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(15.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.38f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    draft: String,
    isSending: Boolean,
    showEmoji: Boolean,
    showAttach: Boolean,
    onDraftChange: (String) -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleAttach: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F0F16),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emoji toggle
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (showEmoji) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f))
                    .clickable { onToggleEmoji() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (showEmoji) "⌨️" else "😊", fontSize = 18.sp)
            }

            // Attach toggle
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (showAttach) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f))
                    .clickable { onToggleAttach() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (showAttach) Icons.Rounded.Close else Icons.Rounded.Add, null, tint = if (showAttach) BrandEnd else Color.White, modifier = Modifier.size(22.dp))
            }

            // Text field
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.07f)) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    singleLine = false,
                    maxLines = 5,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Message…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.28f))
                        inner()
                    }
                )
            }

            // Send button
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(
                        if (draft.isNotBlank()) ConektGradient.brandHorizontal
                        else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f)))
                    )
                    .clickable(enabled = draft.isNotBlank() && !isSending) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Emoji panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF14141E)) {
        Column(Modifier.padding(12.dp)) {
            Text("Emojis", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(bottom = 8.dp))
            EMOJIS.chunked(10).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { emoji ->
                        Text(emoji, fontSize = 26.sp, modifier = Modifier.padding(3.dp).clickable { onPick(emoji) })
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Attach panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttachPanel(onPhoto: () -> Unit, onFile: () -> Unit, onAudio: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF14141E)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AttachItem(Icons.Rounded.Image, "Photo", InfoBlue) { onPhoto() }
            AttachItem(Icons.Rounded.AttachFile, "File", Color(0xFF8B5CF6)) { onFile() }
            AttachItem(Icons.Rounded.Mic, "Audio", SuccessGreen) { onAudio() }
            AttachItem(Icons.Rounded.Headphones, "Music", BrandEnd) {}
        }
    }
}

@Composable
private fun AttachItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.28f), CircleShape).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubbles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(msg: MessageItem, onDelete: () -> Unit) {
    val isMe = msg.isMe
    if (msg.isDeleted) {
        // Deleted message indicator
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.05f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Block, null, tint = Color.White.copy(alpha = 0.28f), modifier = Modifier.size(14.dp))
                    Text("Message deleted", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.30f))
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.widthIn(max = 285.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            when (msg.type) {
                MsgType.TEXT -> TextBubble(msg, isMe, onDelete)
                MsgType.EMOJI -> Text(msg.body ?: "", fontSize = 40.sp, modifier = Modifier.padding(4.dp).combinedClickable(onLongClick = onDelete) {})
                MsgType.IMAGE -> ImageBubble(msg, onDelete)
                MsgType.FILE -> FileBubble(msg, onDelete)
                MsgType.AUDIO -> AudioBubble(msg, isMe, onDelete)
                MsgType.MUSIC -> MusicBubble(msg, isMe, onDelete)
            }
            // Timestamp + read tick
            Row(
                Modifier.padding(horizontal = 4.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
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

@Composable
private fun TextBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = if (isMe) 20.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 20.dp
        ),
        color = if (isMe) BrandEnd else Color.White.copy(alpha = 0.10f)
    ) {
        Text(
            text = msg.body ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .combinedClickable(onLongClick = onDelete) {}
        )
    }
}

@Composable
private fun ImageBubble(msg: MessageItem, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 175.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onLongClick = onDelete) {}
    ) {
        msg.fileUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)))))
    }
}

@Composable
private fun FileBubble(msg: MessageItem, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.08f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Row(Modifier.padding(12.dp).combinedClickable(onLongClick = onDelete) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(InfoBlue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
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

@Composable
private fun AudioBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).combinedClickable(onLongClick = onDelete) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(if (isMe) BrandEnd else Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(8, 14, 10, 18, 12, 8, 16, 10, 14, 8).forEach { h ->
                    Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.55f)))
                }
            }
            Text("0:00", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.50f))
        }
    }
}

@Composable
private fun MusicBubble(msg: MessageItem, isMe: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isMe) BrandEnd.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(12.dp).combinedClickable(onLongClick = onDelete) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(BrandEnd.copy(alpha = 0.20f))) {
                msg.musicCover?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Icon(Icons.Rounded.MusicNote, null, tint = BrandEnd, modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
            Column(Modifier.widthIn(max = 180.dp)) {
                Text(msg.musicTitle ?: "Track", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(msg.musicArtist ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), maxLines = 1)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Misc
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DateDivider(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.07f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.42f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun EmptyThreadHint(otherName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(ConektGradient.brandHorizontal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                "Say hi to ${otherName.substringBefore(" ")} 👋",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.60f)
            )
            Text(
                "Messages are end-to-end encrypted",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.30f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun msgTime(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

private fun msgDate(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val now = java.time.ZonedDateTime.now()
    when {
        zdt.toLocalDate() == now.toLocalDate() -> "Today"
        zdt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}.getOrDefault("")