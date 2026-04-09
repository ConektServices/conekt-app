package com.conekt.suite.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private sealed class DisplayItem {
    data class DateHeader(val label: String) : DisplayItem()
    data class Message(val msg: MessageItem, val showAvatar: Boolean) : DisplayItem()
}

private fun buildMessageDisplayItems(messages: List<MessageItem>): List<DisplayItem> {
    val items = mutableListOf<DisplayItem>()
    var prevDate     = ""
    var prevSenderId = ""

    messages.forEach { msg ->
        val date = msgDate(msg.createdAt)
        if (date != prevDate) {
            items.add(DisplayItem.DateHeader(date))
            prevDate = date
            prevSenderId = "" // reset sender grouping on date change
        }
        val showAvatar = !msg.isMe && msg.senderId != prevSenderId
        prevSenderId = msg.senderId
        items.add(DisplayItem.Message(msg, showAvatar))
    }

    return items
}

private val QUICK_REACTIONS = listOf("❤️", "😂", "😮", "😢", "👍", "👎")

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
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var showProfilePanel by remember { mutableStateOf(false) }
    var showBlockDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId, otherUserId) { vm.init(conversationId, otherUserId) }

    // Auto-scroll to latest
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.sendFile(context, it) }
    }

    // Dismiss keyboard when tapping outside input
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090F))
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        // ── Blur background when context menu is open ──────────────────────
        val blurRadius by animateDpAsState(
            targetValue = if (state.contextMessage != null) 8.dp else 0.dp,
            animationSpec = tween(200),
            label = "blur"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (state.contextMessage != null) Modifier.blur(blurRadius) else Modifier)
        ) {
            ThreadTopBar(
                name               = otherName,
                avatarUrl          = otherAvatarUrl,
                onBack             = onBack,
                onOpenProfilePanel = { showProfilePanel = true }
            )

            // Messages list
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
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (state.messages.isEmpty()) {
                            item(key = "empty") { EmptyThreadHint(otherName = otherName) }
                        } else {
                            // Build display items OUTSIDE composable scope (pure data transform)
                            val displayItems = buildMessageDisplayItems(state.messages)

                            displayItems.forEach { displayItem ->
                                when (displayItem) {
                                    is DisplayItem.DateHeader -> {
                                        item(key = "date_${displayItem.label}") {
                                            DateDivider(displayItem.label)
                                        }
                                    }
                                    is DisplayItem.Message -> {
                                        item(key = displayItem.msg.id) {
                                            MessageBubble(
                                                msg          = displayItem.msg,
                                                showAvatar   = displayItem.showAvatar,
                                                otherName    = otherName,
                                                otherAvatar  = otherAvatarUrl,
                                                onLongPress  = { vm.showContext(displayItem.msg) },
                                                onImageClick = { images, idx -> vm.openImageViewer(images, idx) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emoji picker
            AnimatedVisibility(visible = state.showEmoji, enter = expandVertically(expandFrom = Alignment.Bottom), exit = shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                EmojiPanel(onPick = { emoji -> vm.appendEmoji(emoji) })
            }

            // Attach panel
            AnimatedVisibility(visible = state.showAttach, enter = expandVertically(expandFrom = Alignment.Bottom), exit = shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                AttachPanel(
                    onPhoto = { fileLauncher.launch("image/*") },
                    onFile  = { fileLauncher.launch("*/*") },
                    onAudio = { fileLauncher.launch("audio/*") }
                )
            }

            // Reply banner
            AnimatedVisibility(visible = state.replyingTo != null, enter = expandVertically(expandFrom = Alignment.Bottom), exit = shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                state.replyingTo?.let { reply ->
                    ReplyBanner(reply = reply, onClear = vm::clearReply)
                }
            }

            // Input bar
            MessageInputBar(
                draft          = state.draft,
                isSending      = state.isSending,
                showEmoji      = state.showEmoji,
                showAttach     = state.showAttach,
                onDraftChange  = vm::onDraftChange,
                onToggleEmoji  = vm::toggleEmoji,
                onToggleAttach = vm::toggleAttach,
                onSend         = vm::sendText
            )
        }

        // ── Context menu overlay (iPhone WhatsApp style) ───────────────────
        state.contextMessage?.let { msg ->
            ContextMenuOverlay(
                msg          = msg,
                otherName    = otherName,
                onDismiss    = vm::dismissContext,
                onReply      = { vm.setReply(msg, "You") },
                onReact      = { emoji -> vm.react(msg, emoji) },
                onDeleteForEveryone = if (msg.isMe) ({ vm.deleteMessageForEveryone(msg.id) }) else null,
                onDeleteForMe       = { vm.deleteMessageForMe(msg.id) }
            )
        }

        // ── Image viewer ───────────────────────────────────────────────────
        if (state.showImageViewer && state.viewerImages.isNotEmpty()) {
            ImageViewerDialog(
                images    = state.viewerImages,
                startIndex = state.viewerIndex,
                onDismiss = vm::closeImageViewer
            )
        }

        // ── Profile side panel ────────────────────────────────────────────
        if (showProfilePanel) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)).clickable { showProfilePanel = false })
        }
        AnimatedVisibility(
            visible  = showProfilePanel,
            enter    = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280, easing = FastOutSlowInEasing)),
            exit     = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220, easing = FastOutLinearInEasing)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ProfileSidePanel(
                name          = otherName,
                avatarUrl     = otherAvatarUrl,
                conversationId = conversationId,
                onClose       = { showProfilePanel = false },
                onViewProfile = { showProfilePanel = false; onOpenProfile(otherUserId) },
                onBlock       = { showBlockDialog = true },
                onDelete      = { vm.blockUser(conversationId, false); onBack() }
            )
        }

        // ── Block confirmation ────────────────────────────────────────────
        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title            = { Text("Block $otherName?") },
                text             = { Text("They won't be able to send you messages and you won't see their messages.") },
                confirmButton    = {
                    TextButton(onClick = { vm.blockUser(conversationId, true); showBlockDialog = false; onBack() }) {
                        Text("Block", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton    = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTINUATION of ChatThreadScreen.kt — paste after the main composable
// All private composables used by ChatThreadScreen
// ─────────────────────────────────────────────────────────────────────────────

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ThreadTopBar(name: String, avatarUrl: String?, onBack: () -> Unit, onOpenProfilePanel: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0F0F16), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f)).clickable { onOpenProfilePanel() }) {
                avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = BrandEnd, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable { onOpenProfilePanel() }) {
                Text(name, fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Lock, null, tint = SuccessGreen.copy(alpha = 0.75f), modifier = Modifier.size(11.dp))
                    Text("End-to-end encrypted", style = MaterialTheme.typography.labelSmall, color = SuccessGreen.copy(alpha = 0.65f))
                }
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable { onOpenProfilePanel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    msg: MessageItem,
    showAvatar: Boolean,
    otherName: String,
    otherAvatar: String?,
    onLongPress: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit
) {
    if (msg.isDeleted) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for other person
        if (!msg.isMe) {
            if (showAvatar) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(BrandEnd.copy(alpha = 0.20f))) {
                    otherAvatar?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(otherName.first().uppercaseChar().toString(), color = BrandEnd, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                }
            } else {
                Spacer(Modifier.width(32.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 285.dp),
            horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start
        ) {
            // Reply preview bar
            msg.replyTo?.let { reply ->
                Surface(
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(BrandEnd))
                        Column {
                            Text(
                                if (reply.isMe) "You" else reply.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandEnd,
                                fontWeight = FontWeight.Bold
                            )
                            Text(reply.preview, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Main bubble
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
            ) {
                when (msg.type) {
                    MsgType.TEXT  -> TextBubble(msg)
                    MsgType.EMOJI -> Text(msg.body ?: "", fontSize = 40.sp, modifier = Modifier.padding(4.dp))
                    MsgType.IMAGE -> ImageBubble(msg, onImageClick)
                    MsgType.FILE  -> FileBubble(msg)
                    MsgType.AUDIO -> AudioBubble(msg)
                    MsgType.MUSIC -> MusicBubble(msg)
                }
            }

            // Reactions row
            if (msg.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    msg.reactions.entries.sortedByDescending { it.value }.take(5).forEach { (emoji, count) ->
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.12f)) {
                            Text("$emoji $count", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            // Timestamp + read tick
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(msgTime(msg.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.28f))
                if (msg.isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.DoneAll, null, tint = BrandEnd.copy(alpha = 0.60f), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun TextBubble(msg: MessageItem) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (msg.isMe) 20.dp else 4.dp, bottomEnd = if (msg.isMe) 4.dp else 20.dp),
        color = if (msg.isMe) BrandEnd else Color.White.copy(alpha = 0.10f)
    ) {
        Text(text = msg.body ?: "", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun ImageBubble(msg: MessageItem, onImageClick: (List<String>, Int) -> Unit) {
    val url = msg.fileUrl ?: return
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 175.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onImageClick(listOf(url), 0) }
    ) {
        AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)))))
        Icon(Icons.Rounded.ZoomIn, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(18.dp))
    }
}

@Composable
private fun FileBubble(msg: MessageItem) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.08f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
private fun AudioBubble(msg: MessageItem) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (msg.isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(if (msg.isMe) BrandEnd else Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
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
private fun MusicBubble(msg: MessageItem) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (msg.isMe) BrandEnd.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f), border = BorderStroke(1.dp, if (msg.isMe) BrandEnd.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

// ── Context menu (iPhone WhatsApp style) ──────────────────────────────────────

@Composable
private fun ContextMenuOverlay(
    msg: MessageItem,
    otherName: String,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onDeleteForEveryone: (() -> Unit)?,
    onDeleteForMe: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick reactions
            Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFF1E1E2A), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Text(emoji, fontSize = 28.sp, modifier = Modifier.clickable { onReact(emoji) })
                    }
                    // More reactions button
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // The message itself (preview)
            Box(modifier = Modifier.fillMaxWidth(0.85f)) {
                when (msg.type) {
                    MsgType.TEXT  -> TextBubble(msg)
                    MsgType.IMAGE -> ImageBubble(msg) { _, _ -> }
                    MsgType.AUDIO -> AudioBubble(msg)
                    MsgType.MUSIC -> MusicBubble(msg)
                    else          -> TextBubble(msg.copy(body = "📎 ${msg.fileName ?: msg.type.name}"))
                }
            }

            // Action menu
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E2A), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))) {
                Column(Modifier.fillMaxWidth(0.85f)) {
                    ContextAction(Icons.Rounded.Reply, "Reply") { onReply(); onDismiss() }
                    Divider(color = Color.White.copy(alpha = 0.07f))
                    ContextAction(Icons.Rounded.ContentCopy, "Copy") { onDismiss() }
                    if (onDeleteForEveryone != null) {
                        Divider(color = Color.White.copy(alpha = 0.07f))
                        ContextAction(Icons.Rounded.Delete, "Delete for everyone", tint = Color(0xFFFF4444)) {
                            onDeleteForEveryone()
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.07f))
                    ContextAction(Icons.Rounded.DeleteForever, "Delete for me", tint = Color(0xFFFF4444)) { onDeleteForMe() }
                }
            }
        }
    }
}

@Composable
private fun ContextAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint.copy(alpha = 0.80f), modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = FontWeight.Medium)
    }
}

// ── Image viewer dialog ───────────────────────────────────────────────────────

@Composable
private fun ImageViewerDialog(images: List<String>, startIndex: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }) {
            val pagerState = rememberPagerState(initialPage = startIndex) { images.size }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model              = images[page],
                        contentDescription = null,
                        modifier           = Modifier.fillMaxWidth(),
                        contentScale       = ContentScale.Fit
                    )
                }
            }
            // Page indicator
            if (images.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(images.size) { i ->
                        Box(
                            Modifier.size(if (i == pagerState.currentPage) 20.dp else 7.dp, 7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.40f))
                        )
                    }
                }
            }
            // Close button
            Box(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                    .size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.60f)).clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            // Image counter
            Text(
                "${pagerState.currentPage + 1} / ${images.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 18.dp)
            )
        }
    }
}

// ── Reply banner ──────────────────────────────────────────────────────────────

@Composable
private fun ReplyBanner(reply: ReplyPreview, onClear: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF14141E)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(BrandEnd))
            Column(Modifier.weight(1f)) {
                Text(if (reply.isMe) "Replying to yourself" else "Replying to ${reply.senderName}", style = MaterialTheme.typography.labelSmall, color = BrandEnd, fontWeight = FontWeight.Bold)
                Text(reply.preview, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable { onClear() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(draft: String, isSending: Boolean, showEmoji: Boolean, showAttach: Boolean, onDraftChange: (String) -> Unit, onToggleEmoji: () -> Unit, onToggleAttach: () -> Unit, onSend: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0F0F16), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (showEmoji) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f)).clickable { onToggleEmoji() }, contentAlignment = Alignment.Center) {
                Text(if (showEmoji) "⌨️" else "😊", fontSize = 18.sp)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (showAttach) BrandEnd.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.07f)).clickable { onToggleAttach() }, contentAlignment = Alignment.Center) {
                Icon(if (showAttach) Icons.Rounded.Close else Icons.Rounded.Add, null, tint = if (showAttach) BrandEnd else Color.White, modifier = Modifier.size(22.dp))
            }
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
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (draft.isNotBlank()) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f)))).clickable(enabled = draft.isNotBlank() && !isSending) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Profile side panel ────────────────────────────────────────────────────────

@Composable
private fun ProfileSidePanel(name: String, avatarUrl: String?, conversationId: String, onClose: () -> Unit, onViewProfile: () -> Unit, onBlock: () -> Unit, onDelete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxHeight().width(290.dp), shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp), color = Color(0xFF111120), shadowElevation = 32.dp, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp).background(Brush.radialGradient(listOf(BrandEnd.copy(alpha = 0.20f), Color.Transparent))))
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).clickable { onClose() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(80.dp).background(Brush.linearGradient(listOf(BrandStart, BrandEnd)), CircleShape).padding(3.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF111120)).padding(2.dp).clip(CircleShape)) {
                        avatarUrl?.ifBlank { null }?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            ?: Box(Modifier.fillMaxSize().background(BrandEnd.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) { Text(name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                Spacer(Modifier.height(20.dp))

                Spacer(Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PanelAction(Icons.Rounded.Person, "View Profile", Color.White) { onViewProfile() }
                    PanelAction(Icons.Rounded.Block, "Block User", Color(0xFFFF4444)) { onBlock() }
                    PanelAction(Icons.Rounded.Delete, "Delete Chat", Color(0xFFFF4444)) { onDelete() }
                }
            }
        }
    }
}

@Composable
private fun PanelAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = tint.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Emoji panel ───────────────────────────────────────────────────────────────

@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF14141E)) {
        Column(Modifier.padding(12.dp)) {
            Text("Emojis", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(bottom = 8.dp))
            EMOJIS.chunked(10).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { emoji -> Text(emoji, fontSize = 26.sp, modifier = Modifier.padding(3.dp).clickable { onPick(emoji) }) }
                }
            }
        }
    }
}

// ── Attach panel ──────────────────────────────────────────────────────────────

@Composable
private fun AttachPanel(onPhoto: () -> Unit, onFile: () -> Unit, onAudio: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF14141E)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AttachItem(Icons.Rounded.Image,      "Photo",  InfoBlue)           { onPhoto() }
            AttachItem(Icons.Rounded.AttachFile, "File",   Color(0xFF8B5CF6))  { onFile() }
            AttachItem(Icons.Rounded.Mic,        "Audio",  SuccessGreen)       { onAudio() }
            AttachItem(Icons.Rounded.Headphones, "Music",  BrandEnd)           {}
        }
    }
}

@Composable
private fun AttachItem(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)).border(1.dp, accent.copy(alpha = 0.28f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
    }
}

// ── Misc helpers ──────────────────────────────────────────────────────────────

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
    Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(ConektGradient.brandHorizontal), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text("Say hi to ${otherName.substringBefore(" ")} 👋", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.60f))
            Text("Messages are end-to-end encrypted", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.30f), textAlign = TextAlign.Center)
        }
    }
}

private fun msgTime(iso: String): String = runCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

private fun msgDate(iso: String): String = runCatching {
    val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
    val today = LocalDate.now(ZoneId.systemDefault())
    when (zdt.toLocalDate()) {
        today            -> "Today"
        today.minusDays(1) -> "Yesterday"
        else             -> zdt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }
}.getOrDefault("")