package com.conekt.suite.feature.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient
import com.conekt.suite.ui.theme.InfoBlue
import com.conekt.suite.ui.theme.SuccessGreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardCapitalization

private enum class CanvasTopTab {
    NOTES, PLANNER, POSTED
}

private enum class NoteVisibility {
    PRIVATE, PUBLIC
}

private data class NoteStatUi(
    val title: String,
    val count: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

private data class BoardUi(
    val title: String,
    val subtitle: String,
    val tag: String,
    val accent: Color
)

private data class NoteCardUi(
    val title: String,
    val body: String,
    val meta: String,
    val accent: Color,
    val pinned: Boolean = false
)

private data class TimelineDateUi(
    val day: String,
    val date: String,
    val selected: Boolean = false
)

private data class TimelineEntryUi(
    val time: String,
    val title: String,
    val body: String,
    val accent: Color
)

private data class PostedNoteUi(
    val author: String,
    val handle: String,
    val avatarUrl: String,
    val title: String,
    val body: String,
    val tag: String,
    val stats: String
)

@Composable
fun CanvasScreen(
    onMenuClick: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(CanvasTopTab.NOTES) }
    var showEditorScreen by rememberSaveable { mutableStateOf(false) }
    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteBody by rememberSaveable { mutableStateOf("") }
    var isPublicNote by rememberSaveable { mutableStateOf(false) }

    val stats = listOf(
        NoteStatUi(
            title = "Today",
            count = "6",
            subtitle = "active notes",
            icon = Icons.Rounded.Schedule,
            color = Color(0xFFB9E86C)
        ),
        NoteStatUi(
            title = "Pinned",
            count = "10",
            subtitle = "important",
            icon = Icons.Rounded.Star,
            color = Color(0xFFFF8B6E)
        ),
        NoteStatUi(
            title = "Planned",
            count = "8",
            subtitle = "scheduled",
            icon = Icons.Rounded.CalendarMonth,
            color = Color(0xFFF2D458)
        )
    )

    val boards = listOf(
        BoardUi("Personal Space", "drafts, thoughts, mood", "private", BrandEnd),
        BoardUi("Work Notes", "tasks, plans, meetings", "team", InfoBlue),
        BoardUi("Ideas Dump", "quick captures", "creative", SuccessGreen)
    )

    val notes = listOf(
        NoteCardUi(
            title = "Landing Page Design",
            body = "Refine hero spacing, improve CTA balance, and simplify the first fold so it feels cleaner and more premium.",
            meta = "10:00 pm to 11:00 pm",
            accent = SuccessGreen,
            pinned = true
        ),
        NoteCardUi(
            title = "Prototyping",
            body = "Wire the new Vault and Canvas transitions, tighten the glass bottom nav feel, and review motion speed.",
            meta = "Tomorrow • 09:30 am",
            accent = Color(0xFFF2D458)
        ),
        NoteCardUi(
            title = "Content Notes",
            body = "Turn product ideas into community-ready note posts and structure comments around discussion threads.",
            meta = "Updated 18m ago",
            accent = InfoBlue
        )
    )

    val timelineDates = listOf(
        TimelineDateUi("Mon", "25"),
        TimelineDateUi("Tue", "26"),
        TimelineDateUi("Wed", "27", true),
        TimelineDateUi("Thu", "28"),
        TimelineDateUi("Fri", "29")
    )

    val timelineEntries = listOf(
        TimelineEntryUi(
            time = "09 am",
            title = "Wireframing",
            body = "Sketch out the Canvas note flow and posted note interaction states.",
            accent = BrandEnd
        ),
        TimelineEntryUi(
            time = "11 am",
            title = "UI Design",
            body = "Polish note cards, planner row spacing, and glass layers.",
            accent = Color(0xFF8E7CFF)
        ),
        TimelineEntryUi(
            time = "02 pm",
            title = "Prototyping",
            body = "Connect actions for new note, pin, schedule, and post.",
            accent = Color(0xFFF2D458)
        ),
        TimelineEntryUi(
            time = "04 pm",
            title = "Usability Testing",
            body = "Review how easy it feels to move between notes, planner, and posted notes.",
            accent = Color(0xFF57F2C4)
        ),
        TimelineEntryUi(
            time = "06 pm",
            title = "Meeting",
            body = "Discuss note sharing and community engagement flow.",
            accent = Color(0xFFFF6B8A)
        )
    )

    val postedNotes = listOf(
        PostedNoteUi(
            author = "Elena Juni",
            handle = "@elena.juni",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
            title = "Digital spaces should feel calmer",
            body = "Notes should not feel isolated from the rest of the product. When done well, a note can become a post, a discussion, or a personal archive without friction.",
            tag = "thought",
            stats = "82 reactions • 21 comments"
        ),
        PostedNoteUi(
            author = "Daniel Moss",
            handle = "@daniel.moss",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
            title = "Shared notes need structure",
            body = "The best note systems let users keep things private first, then publish intentionally with context, tags, and room for community comments.",
            tag = "note post",
            stats = "64 reactions • 16 comments"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!showEditorScreen) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 166.dp,
                    bottom = 176.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (selectedTab) {
                    CanvasTopTab.NOTES -> {
                        item { CanvasHeroCard() }
                        item { CanvasSearchBar() }
                        item { NoteStatsSection(stats) }
                        item { BoardsSection(boards) }
                        item { TodayNotesSection(notes) }
                        item {
                            Column {
                                SectionTitle("Posted notes")
                                Spacer(modifier = Modifier.height(12.dp))
                                postedNotes.take(1).forEach { note ->
                                    PostedNoteCard(note)
                                }
                            }
                        }
                    }

                    CanvasTopTab.PLANNER -> {
                        item { PlannerHeaderCard() }
                        item { TimelineDateStrip(timelineDates) }
                        item { TimelineSection(timelineEntries) }
                    }

                    CanvasTopTab.POSTED -> {
                        item {
                            Column {
                                SectionTitle("Community notes")
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(postedNotes) { note ->
                            PostedNoteCard(note)
                        }
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
                                Color.Black.copy(alpha = 0.36f),
                                Color.Black.copy(alpha = 0.80f)
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
                CanvasHeader()
                Spacer(modifier = Modifier.height(10.dp))
                CanvasTopTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            CanvasFab(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 22.dp, bottom = 92.dp),
                onClick = { showEditorScreen = true }
            )
        } else {
            FullScreenNoteEditor(
                title = noteTitle,
                onTitleChange = { noteTitle = it },
                body = noteBody,
                onBodyChange = { noteBody = it },
                isPublic = isPublicNote,
                onVisibilityChange = { isPublicNote = it },
                onBack = { showEditorScreen = false }
            )
        }
    }
}

@Composable
private fun CanvasHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Canvas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "notes, planner, and posted thoughts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Search",
                onClick = {}
            )
            GlassCircleButton(
                icon = Icons.Rounded.NotificationsNone,
                contentDescription = "Notifications",
                onClick = {}
            )
        }
    }
}

@Composable
private fun CanvasTopTabs(
    selectedTab: CanvasTopTab,
    onTabSelected: (CanvasTopTab) -> Unit
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
            CanvasTopTab.entries.forEach { tab ->
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
private fun CanvasHeroCard() {
    Card(
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
                            BrandStart.copy(alpha = 0.12f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Good morning, Byron",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You have 24 notes this week ✍️",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = BrandEnd
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    visibility: NoteVisibility,
    onVisibilityChange: (NoteVisibility) -> Unit
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
                            BrandStart.copy(alpha = 0.14f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit note",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "draft, refine, and choose who can see it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier.clickable { onToggleExpanded() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                    ) {
                        Text(
                            text = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        NoteVisibilityToggle(
                            visibility = visibility,
                            onVisibilityChange = onVisibilityChange
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TextField(
                            value = title,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "Note title",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = BrandEnd
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TextField(
                            value = body,
                            onValueChange = onBodyChange,
                            placeholder = {
                                Text(
                                    text = "Write your note here...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 150.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = BrandEnd
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorActionButton(
                                text = "Save draft",
                                filled = false,
                                modifier = Modifier.weight(1f)
                            )

                            EditorActionButton(
                                text = if (visibility == NoteVisibility.PUBLIC) "Post note" else "Keep private",
                                filled = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteVisibilityToggle(
    visibility: NoteVisibility,
    onVisibilityChange: (NoteVisibility) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NoteVisibility.entries.forEach { item ->
                val selected = visibility == item

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
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
                        .clickable { onVisibilityChange(item) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item == NoteVisibility.PRIVATE) "PRIVATE" else "PUBLIC",
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
private fun EditorActionButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { },
        shape = RoundedCornerShape(18.dp),
        color = if (filled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
        border = if (filled) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (filled) {
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ConektGradient.brandHorizontal)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CanvasSearchBar() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Search notes, plans, boards...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteStatsSection(
    stats: List<NoteStatUi>
) {
    Column {
        SectionTitle("This week")
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.forEach { stat ->
                StatCard(
                    modifier = Modifier.weight(1f),
                    stat = stat
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    stat: NoteStatUi
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = stat.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = stat.title,
                    tint = Color.Black
                )
            }

            Text(
                text = stat.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = stat.count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = stat.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BoardsSection(
    boards: List<BoardUi>
) {
    Column {
        SectionTitle("Boards")
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(boards) { board ->
                BoardCard(board)
            }
        }
    }
}

@Composable
private fun BoardCard(
    board: BoardUi
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            board.accent.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                ) {
                    Text(
                        text = board.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = board.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp)
                )

                Text(
                    text = board.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniBoardDot(board.accent)
                    MiniBoardDot(board.accent.copy(alpha = 0.70f))
                    MiniBoardDot(board.accent.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun MiniBoardDot(
    color: Color
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun TodayNotesSection(
    notes: List<NoteCardUi>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Today’s notes")
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "See all",
                style = MaterialTheme.typography.bodySmall,
                color = BrandEnd
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        notes.forEachIndexed { index, note ->
            TodayNoteCard(note)
            if (index != notes.lastIndex) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TodayNoteCard(
    note: NoteCardUi
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = BrandEnd
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(note.accent)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = note.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = note.accent
                )
            }
        }
    }
}

@Composable
private fun PlannerHeaderCard() {
    Card(
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
                            InfoBlue.copy(alpha = 0.12f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandEnd
                    )
                    Text(
                        text = "Welcome Byron",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add task",
                            tint = BrandEnd,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add note",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "March 2024",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 66.dp)
            )
        }
    }
}

@Composable
private fun TimelineDateStrip(
    dates: List<TimelineDateUi>
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dates.forEach { date ->
            TimelineDateChip(date)
        }
    }
}

@Composable
private fun TimelineDateChip(
    date: TimelineDateUi
) {
    val background =
        if (date.selected) Brush.verticalGradient(listOf(Color(0xFF6C6BFF), Color(0xFF8E7CFF)))
        else Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = if (date.selected) 0f else 0.12f),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.day,
                style = MaterialTheme.typography.labelMedium,
                color = if (date.selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date.date,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (date.selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimelineSection(
    entries: List<TimelineEntryUi>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEachIndexed { index, entry ->
            TimelineEntryCard(entry)

            if (index == 1) {
                LunchBreakLine()
            }
        }
    }
}

@Composable
private fun TimelineEntryCard(
    entry: TimelineEntryUi
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp).padding(top = 10.dp)
        )

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(entry.accent)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entry.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LunchBreakLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(52.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFF7C61FF).copy(alpha = 0.45f))
            )
            Text(
                text = "Lunch",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF7C61FF),
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFF7C61FF).copy(alpha = 0.45f))
            )
        }
    }
}

@Composable
private fun PostedNoteCard(
    note: PostedNoteUi
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = note.avatarUrl,
                    contentDescription = note.author,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = note.author,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = note.handle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = note.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            Text(
                text = note.stats,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )

            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPill(Icons.Rounded.FavoriteBorder, "Like")
                ActionPill(Icons.Rounded.ChatBubbleOutline, "Comment")
                ActionPill(Icons.Rounded.Send, "Share")
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            shape = RoundedCornerShape(18.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CanvasFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
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
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = "New note",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FullScreenNoteEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 132.dp,
                bottom = 170.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NoteEditorHero()
            }

            item {
                NoteVisibilitySwitcher(
                    isPublic = isPublic,
                    onVisibilityChange = onVisibilityChange
                )
            }

            item {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Title",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandEnd
                    )
                )
            }

            item {
                TextField(
                    value = body,
                    onValueChange = onBodyChange,
                    placeholder = {
                        Text(
                            text = "Start writing your thoughts...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 420.dp),
                    shape = RoundedCornerShape(30.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandEnd
                    )
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorBottomButton(
                        text = "Save draft",
                        filled = false,
                        modifier = Modifier.weight(1f)
                    )
                    EditorBottomButton(
                        text = if (isPublic) "Post note" else "Keep private",
                        filled = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.42f),
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
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.78f)
                        )
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
            GlassCircleButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Write note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "full-page editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GlassCircleButton(
                icon = Icons.Rounded.NotificationsNone,
                contentDescription = "Options",
                onClick = {}
            )
        }
    }
}

@Composable
private fun NoteEditorHero() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            BrandStart.copy(alpha = 0.14f),
                            BrandEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "Capture the idea while it is fresh",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Write privately or publish it to your shared space.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteVisibilitySwitcher(
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisibilityTab(
                selected = !isPublic,
                label = "PRIVATE",
                icon = Icons.Rounded.Lock,
                onClick = { onVisibilityChange(false) },
                modifier = Modifier.weight(1f)
            )
            VisibilityTab(
                selected = isPublic,
                label = "PUBLIC",
                icon = Icons.Rounded.Public,
                onClick = { onVisibilityChange(true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VisibilityTab(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) ConektGradient.brandHorizontal
                else Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditorBottomButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { },
        shape = RoundedCornerShape(18.dp),
        color = if (filled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = if (filled) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (filled) {
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ConektGradient.brandHorizontal)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
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