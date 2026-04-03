package com.conekt.suite.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMusicSheet(
    state: ShareMusicState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onCoverPicked: (Uri) -> Unit,
    onPublicToggle: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.visible) return

    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onCoverPicked(it) } }

    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor     = Color(0xFF0D0E14),
        dragHandle         = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Share to Conekt",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        text  = "let others stream your music",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.48f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint               = Color.White.copy(alpha = 0.60f),
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            // Cover + Track info row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Cover picker
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .clickable { coverLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.coverUri != null) {
                        AsyncImage(
                            model              = state.coverUri,
                            contentDescription = "Cover",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.AddAPhoto,
                                contentDescription = null,
                                tint               = Color.White.copy(alpha = 0.40f),
                                modifier           = Modifier.size(22.dp)
                            )
                            Text(
                                text  = "Cover",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.30f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text  = state.localTrack?.title ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text  = state.localTrack?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.56f)
                    )
                    state.localTrack?.let {
                        Text(
                            text  = formatDuration(it.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandEnd
                        )
                    }
                }
            }

            // Title field
            SheetField(
                value       = state.title,
                onValueChange = onTitleChange,
                label       = "Track title",
                icon        = Icons.Rounded.MusicNote,
                capitalization = KeyboardCapitalization.Words
            )

            // Artist field
            SheetField(
                value       = state.artist,
                onValueChange = onArtistChange,
                label       = "Artist name",
                icon        = Icons.Rounded.Person,
                capitalization = KeyboardCapitalization.Words
            )

            // Genre field
            SheetField(
                value       = state.genre,
                onValueChange = onGenreChange,
                label       = "Genre (optional)",
                icon        = Icons.Rounded.LibraryMusic
            )

            // Public toggle
            Surface(
                shape  = RoundedCornerShape(20.dp),
                color  = Color.White.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (state.isPublic) SuccessGreen.copy(alpha = 0.16f) else BrandEnd.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (state.isPublic) SuccessGreen else BrandEnd,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = if (state.isPublic) "Public" else "Private",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, color = Color.White
                        )
                        Text(
                            text = if (state.isPublic) "Anyone can stream this" else "Only you can play this",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.46f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked       = state.isPublic,
                        onCheckedChange = { onPublicToggle() },
                        colors        = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = SuccessGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
                        )
                    )
                }
            }

            // Error
            AnimatedVisibility(visible = state.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = BrandEnd.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandEnd.copy(alpha = 0.28f))
                ) {
                    Text(
                        text     = state.errorMessage.orEmpty(),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFFFF8B8B),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Upload button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ConektGradient.brandHorizontal)
                    .clickable(enabled = !state.isUploading) { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                if (state.isUploading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(text = "Upload & Share", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(label, color = Color.White.copy(alpha = 0.28f)) },
        leadingIcon   = { Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(20.dp)) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(18.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = capitalization),
        colors        = TextFieldDefaults.colors(
            focusedContainerColor   = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor             = BrandEnd,
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White.copy(alpha = 0.88f)
        )
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}