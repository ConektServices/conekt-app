package com.conekt.suite.feature.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

private enum class VaultTopTab {
    ALL, GALLERY, DOCS, SHARED
}

private data class VaultShortcutUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val previewImages: List<String> = emptyList(),
    val previewRows: List<String> = emptyList()
)

private enum class VaultManagerFilter {
    ALL, RECENT, FOLDERS
}

private data class VaultManagerItemUi(
    val name: String,
    val meta: String,
    val icon: ImageVector,
    val accent: Color,
    val trailing: String,
    val isFolder: Boolean
)

private data class VaultFolderUi(
    val title: String,
    val meta: String,
    val access: String,
    val accent: Color
)

private data class VaultFileUi(
    val name: String,
    val meta: String,
    val icon: ImageVector,
    val accent: Color,
    val trailing: String
)

private data class SharedItemUi(
    val title: String,
    val author: String,
    val avatarUrl: String,
    val access: String,
    val icon: ImageVector
)

@Composable
fun VaultScreen(
    onMenuClick: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(VaultTopTab.ALL) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFileManager by rememberSaveable { mutableStateOf(false) }
    var managerFilter by rememberSaveable { mutableStateOf(VaultManagerFilter.ALL) }

    val contentTopPadding = when {
        showFileManager && searchExpanded -> 194.dp
        showFileManager -> 154.dp
        searchExpanded -> 228.dp
        else -> 178.dp
    }

    val shortcuts = listOf(
        VaultShortcutUi(
            title = "Gallery",
            subtitle = "1,284 items",
            icon = Icons.Rounded.Collections,
            gradient = listOf(BrandStart, BrandEnd),
            previewImages = listOf(
                "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=500&q=80",
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=500&q=80",
                "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=500&q=80"
            )
        ),
        VaultShortcutUi(
            title = "Music",
            subtitle = "86 recent tracks",
            icon = Icons.Rounded.Headphones,
            gradient = listOf(InfoBlue, BrandEnd),
            previewImages = listOf(
                "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=500&q=80",
                "https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=500&q=80"
            )
        ),
        VaultShortcutUi(
            title = "Docs",
            subtitle = "34 recent files",
            icon = Icons.Rounded.Description,
            gradient = listOf(SuccessGreen, InfoBlue),
            previewRows = listOf(
                "Q2 Strategy Deck.pdf",
                "onboarding_copy.docx",
                "pricing_notes.txt"
            )
        ),
        VaultShortcutUi(
            title = "Private",
            subtitle = "Locked space",
            icon = Icons.Rounded.Lock,
            gradient = listOf(BrandEnd, Color(0xFF9B4DFF)),
            previewRows = listOf(
                "Contracts",
                "Passwords",
                "Archived"
            )
        )
    )

    val folders = listOf(
        VaultFolderUi(
            title = "Brand shoots",
            meta = "184 items • updated 12m ago",
            access = "Cloud",
            accent = BrandEnd
        ),
        VaultFolderUi(
            title = "Pitch deck assets",
            meta = "29 files • shared",
            access = "Shared",
            accent = InfoBlue
        ),
        VaultFolderUi(
            title = "Weekend gallery",
            meta = "76 photos • synced",
            access = "Synced",
            accent = SuccessGreen
        ),
        VaultFolderUi(
            title = "Contracts",
            meta = "11 documents • encrypted",
            access = "Private",
            accent = Color(0xFF9B4DFF)
        ),
        VaultFolderUi(
            title = "Audio drafts",
            meta = "23 tracks • online",
            access = "Music",
            accent = Color(0xFFFFB14D)
        )
    )

    val recentFiles = listOf(
        VaultFileUi(
            name = "Q2 Strategy Deck.pdf",
            meta = "PDF • 12.4 MB • 8m ago",
            icon = Icons.Rounded.Description,
            accent = BrandEnd,
            trailing = "Pinned"
        ),
        VaultFileUi(
            name = "IMG_2038.jpg",
            meta = "Image • 4.1 MB • 16m ago",
            icon = Icons.Rounded.Image,
            accent = InfoBlue,
            trailing = "Gallery"
        ),
        VaultFileUi(
            name = "voice_note_mix.mp3",
            meta = "Audio • 7.8 MB • 28m ago",
            icon = Icons.Rounded.Headphones,
            accent = SuccessGreen,
            trailing = "Music"
        ),
        VaultFileUi(
            name = "onboarding_copy.docx",
            meta = "Document • 1.2 MB • 54m ago",
            icon = Icons.Rounded.InsertDriveFile,
            accent = Color(0xFF9B4DFF),
            trailing = "Docs"
        )
    )

    val sharedItems = listOf(
        SharedItemUi(
            title = "Campaign visuals",
            author = "Elena Juni",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
            access = "Can edit",
            icon = Icons.Rounded.Collections
        ),
        SharedItemUi(
            title = "Legal docs",
            author = "Daniel Moss",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
            access = "View only",
            icon = Icons.Rounded.Description
        )
    )

    val galleryPhotos = listOf(
        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1526772662000-3f88f10405ff?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=80"
    )

    val managerItems = listOf(
        VaultManagerItemUi(
            name = "Brand shoots",
            meta = "Folder • 184 items • updated 12m ago",
            icon = Icons.Rounded.Folder,
            accent = BrandEnd,
            trailing = "Cloud",
            isFolder = true
        ),
        VaultManagerItemUi(
            name = "Pitch deck assets",
            meta = "Folder • 29 files • shared with team",
            icon = Icons.Rounded.Folder,
            accent = InfoBlue,
            trailing = "Shared",
            isFolder = true
        ),
        VaultManagerItemUi(
            name = "Weekend gallery",
            meta = "Folder • 76 photos • synced",
            icon = Icons.Rounded.Folder,
            accent = SuccessGreen,
            trailing = "Synced",
            isFolder = true
        ),
        VaultManagerItemUi(
            name = "Q2 Strategy Deck.pdf",
            meta = "PDF • 12.4 MB",
            icon = Icons.Rounded.Description,
            accent = BrandEnd,
            trailing = "8m ago",
            isFolder = false
        ),
        VaultManagerItemUi(
            name = "IMG_2038.jpg",
            meta = "Image • 4.1 MB",
            icon = Icons.Rounded.Image,
            accent = InfoBlue,
            trailing = "16m ago",
            isFolder = false
        ),
        VaultManagerItemUi(
            name = "voice_note_mix.mp3",
            meta = "Audio • 7.8 MB",
            icon = Icons.Rounded.Headphones,
            accent = SuccessGreen,
            trailing = "28m ago",
            isFolder = false
        ),
        VaultManagerItemUi(
            name = "onboarding_copy.docx",
            meta = "Document • 1.2 MB",
            icon = Icons.Rounded.InsertDriveFile,
            accent = Color(0xFF9B4DFF),
            trailing = "54m ago",
            isFolder = false
        )
    )

    val filteredManagerItems = when (managerFilter) {
        VaultManagerFilter.ALL -> managerItems
        VaultManagerFilter.RECENT -> managerItems.filterNot { it.isFolder }
        VaultManagerFilter.FOLDERS -> managerItems.filter { it.isFolder }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!showFileManager) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = contentTopPadding,
                    bottom = 176.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    StorageHeroCard(
                        onOpenManager = { showFileManager = true }
                    )
                }

                when (selectedTab) {
                    VaultTopTab.ALL -> {
                        item { VaultShortcutsSection(shortcuts) }
                        item { FolderOverviewSection(folders.take(3)) }
                        item { AllOnlineFoldersSection(folders) }
                        item { RecentFilesSection(recentFiles) }
                        item {
                            GalleryMasonryHorizontalSection(
                                title = "Gallery view",
                                photos = galleryPhotos
                            )
                        }
                    }

                    VaultTopTab.GALLERY -> {
                        item {
                            GalleryMasonryHorizontalSection(
                                title = "Gallery stream",
                                photos = galleryPhotos + galleryPhotos.take(4)
                            )
                        }
                        item {
                            AllOnlineFoldersSection(
                                folders.filter { it.access == "Cloud" || it.access == "Synced" }
                            )
                        }
                    }

                    VaultTopTab.DOCS -> {
                        item {
                            VaultShortcutsSection(
                                shortcuts.filter { it.title == "Docs" || it.title == "Private" }
                            )
                        }
                        item {
                            AllOnlineFoldersSection(
                                folders.filter { it.access == "Private" || it.title.contains("Contract", true) }
                            )
                        }
                        item {
                            RecentFilesSection(
                                recentFiles.filter { it.trailing == "Docs" || it.name.endsWith(".pdf") }
                            )
                        }
                    }

                    VaultTopTab.SHARED -> {
                        item { SharedSection(sharedItems) }
                        item {
                            AllOnlineFoldersSection(
                                folders.filter { it.access == "Shared" }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = contentTopPadding,
                    bottom = 170.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FileManagerHeroCard(
                        onClose = { showFileManager = false }
                    )
                }

                item {
                    FileManagerFilterTabs(
                        selectedFilter = managerFilter,
                        onFilterSelected = { managerFilter = it }
                    )
                }

                item {
                    FileManagerPathCard()
                }

                item {
                    FileManagerStorageRow()
                }

                item {
                    SectionTitle("All files")
                }

                items(filteredManagerItems.size) { index ->
                    FileManagerEntryRow(
                        item = filteredManagerItems[index]
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            VaultHeader(
                searchExpanded = searchExpanded,
                onSearchClick = { searchExpanded = !searchExpanded }
            )

            AnimatedVisibility(visible = searchExpanded) {
                SearchInputCard(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            if (!showFileManager) {
                Spacer(modifier = Modifier.height(12.dp))

                VaultTopTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        GradientUploadFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 92.dp)
        )
    }
}

@Composable
private fun VaultHeader(
    searchExpanded: Boolean,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vault",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "your files, gallery, docs, and media",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(
                icon = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (searchExpanded) "Close search" else "Search",
                onClick = onSearchClick
            )
            GlassCircleButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = "More",
                onClick = {}
            )
        }
    }
}

@Composable
private fun SearchInputCard(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search files, folders, tags...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = BrandEnd
            )
        )
    }
}

@Composable
private fun VaultTopTabs(
    selectedTab: VaultTopTab,
    onTabSelected: (VaultTopTab) -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        tonalElevation = 0.dp,
        shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VaultTopTab.entries.forEach { tab ->
                val selected = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) {
                                ConektGradient.brandHorizontal
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                                    )
                                )
                            }
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageHeroCard(
    onOpenManager: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenManager() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.18f),
                            BrandEnd.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ConektGradient.brandHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = "Vault",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Your digital space",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "2.4 GB of 5 GB used",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Sort"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height(12.dp)
                            .background(ConektGradient.brandHorizontal)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultChip("Images 41%")
                    VaultChip("Audio 24%")
                    VaultChip("Docs 13%")
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.clickable { onOpenManager() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Open manager",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultShortcutsSection(
    shortcuts: List<VaultShortcutUi>
) {
    Column {
        SectionTitle("Quick vault")
        Spacer(modifier = Modifier.height(12.dp))

        val firstRow = shortcuts.take(2)
        val secondRow = shortcuts.drop(2).take(2)

        if (firstRow.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                firstRow.forEach { item ->
                    VaultShortcutCard(
                        modifier = Modifier.weight(1f),
                        shortcut = item
                    )
                }
            }
        }

        if (secondRow.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                secondRow.forEach { item ->
                    VaultShortcutCard(
                        modifier = Modifier.weight(1f),
                        shortcut = item
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultShortcutCard(
    modifier: Modifier = Modifier,
    shortcut: VaultShortcutUi
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            shortcut.gradient.first().copy(alpha = 0.22f),
                            shortcut.gradient.last().copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.horizontalGradient(shortcut.gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = shortcut.icon,
                            contentDescription = shortcut.title,
                            tint = Color.White
                        )
                    }

                    Text(
                        text = shortcut.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = shortcut.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                ShortcutPreviewPane(shortcut = shortcut)
            }
        }
    }
}

@Composable
private fun ShortcutPreviewPane(
    shortcut: VaultShortcutUi
) {
    Surface(
        modifier = Modifier.width(88.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.14f)
    ) {
        if (shortcut.previewImages.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcut.previewImages.take(3).forEachIndexed { index, image ->
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (index == 0) 44.dp else 30.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcut.previewRows.take(3).forEach { row ->
                    ShortcutPreviewLine(row)
                }
            }
        }
    }
}

@Composable
private fun ShortcutPreviewLine(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FileManagerHeroCard(
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.18f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "File manager",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "browse folders, recent files, and synced content",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                GlassCircleButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close manager",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun FileManagerFilterTabs(
    selectedFilter: VaultManagerFilter,
    onFilterSelected: (VaultManagerFilter) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VaultManagerFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) {
                                ConektGradient.brandHorizontal
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                                    )
                                )
                            }
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerPathCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Path",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Conekt / Vault / All files",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FileManagerStorageRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "18",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Files",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "204",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FileManagerEntryRow(
    item: VaultManagerItemUi
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = item.accent
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (item.isFolder) {
                FolderChip(item.trailing, item.accent)
            } else {
                Text(
                    text = item.trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FolderOverviewSection(
    folders: List<VaultFolderUi>
) {
    if (folders.isEmpty()) return

    Column {
        SectionTitle("Folders overview")
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(folders) { folder ->
                FolderOverviewCard(folder)
            }
        }
    }
}

@Composable
private fun FolderOverviewCard(
    folder: VaultFolderUi
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .height(146.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            folder.accent.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(folder.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = folder.title,
                        tint = folder.accent
                    )
                }

                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp)
                )

                Text(
                    text = folder.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                FolderChip(folder.access, folder.accent)
            }
        }
    }
}

@Composable
private fun AllOnlineFoldersSection(
    folders: List<VaultFolderUi>
) {
    if (folders.isEmpty()) return

    Column {
        SectionTitle("All online folders")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                folders.forEachIndexed { index, folder ->
                    OnlineFolderRow(folder)

                    if (index != folders.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineFolderRow(
    folder: VaultFolderUi
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(folder.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = folder.title,
                    tint = folder.accent
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = folder.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            FolderChip(folder.access, folder.accent)
        }
    }
}

@Composable
private fun RecentFilesSection(
    files: List<VaultFileUi>
) {
    if (files.isEmpty()) return

    Column {
        SectionTitle("Recent files")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                files.forEachIndexed { index, file ->
                    FileRow(file)

                    if (index != files.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: VaultFileUi
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(file.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = file.icon,
                    contentDescription = file.name,
                    tint = file.accent
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = file.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            ) {
                Text(
                    text = file.trailing,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GalleryMasonryHorizontalSection(
    title: String,
    photos: List<String>
) {
    if (photos.isEmpty()) return

    val columns = photos.chunked(3)

    Column {
        SectionTitle(title)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(columns) { columnPhotos ->
                GalleryMasonryColumn(columnPhotos)
            }
        }
    }
}

@Composable
private fun GalleryMasonryColumn(
    photos: List<String>
) {
    Column(
        modifier = Modifier.width(168.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        photos.forEachIndexed { index, url ->
            GalleryMasonryCard(
                imageUrl = url,
                cardIndex = index,
                height = when (index % 3) {
                    0 -> 224.dp
                    1 -> 148.dp
                    else -> 188.dp
                }
            )
        }
    }
}

@Composable
private fun GalleryMasonryCard(
    imageUrl: String,
    cardIndex: Int,
    height: androidx.compose.ui.unit.Dp
) {
    val transition = rememberInfiniteTransition(label = "galleryFloat")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (cardIndex % 2 == 0) -8f else 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200 + (cardIndex * 250)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                translationY = offsetY
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Gallery image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.42f)
                            )
                        )
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.28f)
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedSection(
    items: List<SharedItemUi>
) {
    Column {
        SectionTitle("Shared with you")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                items.forEachIndexed { index, item ->
                    SharedRow(item)

                    if (index != items.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedRow(
    item: SharedItemUi
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = item.author,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "from ${item.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.access,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.access,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderChip(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
private fun GradientUploadFab(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ConektGradient.brandHorizontal)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add file",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier.clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VaultChip(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}