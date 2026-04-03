package com.conekt.suite.feature.pulse

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreatePostViewModel = viewModel(
        factory = CreatePostViewModel.Factory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    // Media pickers
    val multiMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> -> if (uris.isNotEmpty()) viewModel.onMediaPicked(uris) }

    val storyMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.onStoryMediaPicked(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07080C),
                        Color(0xFF0C0D13),
                        Color(0xFF0A0B10)
                    )
                )
            )
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandStart.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 80.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Type toggle ───────────────────────────────────────────────────
            item {
                TypeToggle(
                    selected = state.type,
                    onSelect = viewModel::onTypeChange
                )
            }

            // ── Form content (animated on type change) ────────────────────────
            item {
                AnimatedContent(
                    targetState = state.type,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 14 })
                            .togetherWith(fadeOut(tween(160)))
                    },
                    label = "postTypeTransition"
                ) { type ->
                    when (type) {
                        CreatePostType.POST ->
                            PostForm(
                                state          = state,
                                onBodyChange   = viewModel::onBodyChange,
                                onLocationChange = viewModel::onLocationChange,
                                onVisibilityChange = viewModel::onVisibilityChange,
                                onAddMedia     = { multiMediaLauncher.launch("image/*") },
                                onRemoveMedia  = viewModel::removeMedia
                            )
                        CreatePostType.STORY ->
                            StoryForm(
                                state          = state,
                                onCaptionChange = viewModel::onCaptionChange,
                                onPickMedia    = { storyMediaLauncher.launch("image/*") }
                            )
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter   = fadeIn(), exit = fadeOut()
                ) {
                    Surface(
                        shape  = RoundedCornerShape(16.dp),
                        color  = BrandEnd.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, BrandEnd.copy(alpha = 0.28f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = state.errorMessage.orEmpty(),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = Color(0xFFFF8B8B),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text     = "✕",
                                color    = Color(0xFFFF8B8B).copy(alpha = 0.70f),
                                modifier = Modifier.clickable { viewModel.clearError() }
                            )
                        }
                    }
                }
            }
        }

        // ── Top header ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassBtn(Icons.Rounded.ArrowBack, "Back", onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text       = if (state.type == CreatePostType.POST) "New post" else "New story",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text  = if (state.type == CreatePostType.POST)
                        "share with your space" else "disappears in 24 hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.52f)
                )
            }
        }

        // ── Bottom post button ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ConektGradient.brandHorizontal)
                    .clickable(enabled = !state.isPosting) { viewModel.submit() },
                contentAlignment = Alignment.Center
            ) {
                if (state.isPosting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Send,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(20.dp)
                        )
                        Text(
                            text       = if (state.type == CreatePostType.POST) "Post" else "Share story",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Type toggle ───────────────────────────────────────────────────────────────

@Composable
private fun TypeToggle(selected: CreatePostType, onSelect: (CreatePostType) -> Unit) {
    Surface(
        shape  = RoundedCornerShape(22.dp),
        color  = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CreatePostType.entries.forEach { type ->
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        )
                        .clickable { onSelect(type) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (type == CreatePostType.POST) "Post" else "Story",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isSelected) Color.White else Color.White.copy(alpha = 0.46f)
                    )
                }
            }
        }
    }
}

// ── Post form ─────────────────────────────────────────────────────────────────

@Composable
private fun PostForm(
    state: CreatePostUiState,
    onBodyChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onVisibilityChange: (PostVisibility) -> Unit,
    onAddMedia: () -> Unit,
    onRemoveMedia: (Uri) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Media area
        if (state.mediaUris.isEmpty()) {
            // Empty media picker
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onAddMedia() },
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(BrandStart.copy(alpha = 0.40f), BrandEnd.copy(alpha = 0.40f))
                    )
                )
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ConektGradient.brandHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.ImageSearch,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text     = "Add photos",
                        style    = MaterialTheme.typography.titleSmall,
                        color    = Color.White.copy(alpha = 0.80f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text  = "Up to 4 images",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Media grid preview
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.mediaUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            AsyncImage(
                                model              = uri,
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.60f))
                                    .clickable { onRemoveMedia(uri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    tint               = Color.White,
                                    modifier           = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Add more button (if < 4)
                    if (state.mediaUris.size < 4) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { onAddMedia() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Rounded.Add,
                                    contentDescription = "Add more",
                                    tint               = Color.White.copy(alpha = 0.50f),
                                    modifier           = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Body text
        Surface(
            shape  = RoundedCornerShape(24.dp),
            color  = Color.White.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            TextField(
                value       = state.body,
                onValueChange = onBodyChange,
                placeholder = {
                    Text(
                        text  = "What's on your mind?",
                        color = Color.White.copy(alpha = 0.30f)
                    )
                },
                modifier    = Modifier.fillMaxWidth(),
                minLines    = 4,
                maxLines    = 10,
                shape       = RoundedCornerShape(24.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors      = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = BrandEnd,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White.copy(alpha = 0.88f)
                )
            )
        }

        // Visibility selector
        VisibilitySelector(
            selected   = state.visibility,
            onSelect   = onVisibilityChange
        )

        // Location (optional)
        Surface(
            shape  = RoundedCornerShape(20.dp),
            color  = Color.White.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            TextField(
                value       = state.location,
                onValueChange = onLocationChange,
                placeholder = { Text("Add location (optional)", color = Color.White.copy(alpha = 0.30f)) },
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Rounded.Place,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = 0.40f),
                        modifier           = Modifier.size(20.dp)
                    )
                },
                singleLine  = true,
                modifier    = Modifier.fillMaxWidth(),
                shape       = RoundedCornerShape(20.dp),
                colors      = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = BrandEnd,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White.copy(alpha = 0.88f)
                )
            )
        }
    }
}

// ── Story form ────────────────────────────────────────────────────────────────

@Composable
private fun StoryForm(
    state: CreatePostUiState,
    onCaptionChange: (String) -> Unit,
    onPickMedia: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Story media preview / picker
        if (state.storyMediaUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
                    .clip(RoundedCornerShape(34.dp))
                    .clickable { onPickMedia() }
            ) {
                AsyncImage(
                    model              = state.storyMediaUri,
                    contentDescription = "Story media",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
                // Overlay hint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.24f))
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.40f)
                ) {
                    Text(
                        text     = "Tap to change",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            // Empty picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(BrandStart.copy(alpha = 0.40f), BrandEnd.copy(alpha = 0.40f))
                        ),
                        RoundedCornerShape(34.dp)
                    )
                    .clickable { onPickMedia() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ConektGradient.brandHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.ImageSearch,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text       = "Pick story media",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White.copy(alpha = 0.80f)
                    )
                    Text(
                        text      = "Photo or video • disappears in 24h",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = Color.White.copy(alpha = 0.38f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Caption
        Surface(
            shape  = RoundedCornerShape(22.dp),
            color  = Color.White.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            TextField(
                value         = state.caption,
                onValueChange = onCaptionChange,
                placeholder   = { Text("Add a caption...", color = Color.White.copy(alpha = 0.30f)) },
                minLines      = 2,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(22.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = BrandEnd,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White.copy(alpha = 0.88f)
                )
            )
        }
    }
}

// ── Visibility selector ───────────────────────────────────────────────────────

@Composable
private fun VisibilitySelector(selected: PostVisibility, onSelect: (PostVisibility) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text     = "WHO CAN SEE THIS",
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White.copy(alpha = 0.44f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PostVisibility.entries.forEach { vis ->
                val isSelected = vis == selected
                val icon = when (vis) {
                    PostVisibility.PUBLIC    -> Icons.Rounded.Public
                    PostVisibility.FOLLOWERS -> Icons.Rounded.People
                    PostVisibility.PRIVATE   -> Icons.Rounded.Lock
                }
                val accent = when (vis) {
                    PostVisibility.PUBLIC    -> SuccessGreen
                    PostVisibility.FOLLOWERS -> InfoBlue
                    PostVisibility.PRIVATE   -> BrandEnd
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(vis) },
                    shape  = RoundedCornerShape(18.dp),
                    color  = if (isSelected) accent.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) accent.copy(alpha = 0.44f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = vis.label,
                            tint               = if (isSelected) accent else Color.White.copy(alpha = 0.38f),
                            modifier           = Modifier.size(20.dp)
                        )
                        Text(
                            text       = vis.label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) accent else Color.White.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

// ── Glass button ──────────────────────────────────────────────────────────────

@Composable
private fun GlassBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier        = Modifier.size(40.dp),
        shape           = CircleShape,
        color           = Color.White.copy(alpha = 0.08f),
        shadowElevation = 10.dp,
        border          = androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.12f)
        )
    ) {
        Box(modifier = Modifier.clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}