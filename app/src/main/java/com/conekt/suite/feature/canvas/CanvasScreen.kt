package com.conekt.suite.feature.canvas

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.conekt.suite.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ── Tab enum (kept identical to existing UI) ──────────────────────────────────

private enum class CanvasTopTab { NOTES, PLANNER, POSTED }

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CanvasScreen(vm: CanvasViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(CanvasTopTab.NOTES) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Main scrollable content ───────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                start  = 20.dp, end = 20.dp,
                top    = 166.dp,
                bottom = 176.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                CanvasTopTab.NOTES   -> notesTabItems(state, vm)
                CanvasTopTab.PLANNER -> plannerTabItems(state, vm)
                CanvasTopTab.POSTED  -> postedTabItems(state, vm)
            }
        }

        // ── Top gradient scrim ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        // ── Bottom gradient scrim ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f), Color.Black.copy(alpha = 0.80f))
                    )
                )
        )

        // ── Header + tabs ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            CanvasHeader()
            Spacer(Modifier.height(10.dp))
            CanvasTopTabRow(selectedTab) { selectedTab = it }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 92.dp)
        ) {
            CanvasFab(
                selectedTab = selectedTab,
                onNewNote   = vm::openNewNote,
                onNewPlan   = vm::openNewPlanItem
            )
        }

        // ── Note editor (full screen overlay) ─────────────────────────────────
        AnimatedVisibility(
            visible = state.showEditor,
            enter   = slideInVertically { it } + fadeIn(tween(220)),
            exit    = slideOutVertically { it } + fadeOut(tween(180))
        ) {
            NoteEditorScreen(
                note     = state.editorNote,
                isSaving = state.isSaving,
                onSave   = { title, body, color, board, pinned, pub ->
                    vm.saveNote(title, body, color, board, pinned, pub)
                },
                onDismiss = vm::closeEditor
            )
        }

        // ── Planner item editor ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showPlanEditor,
            enter   = slideInVertically { it } + fadeIn(tween(220)),
            exit    = slideOutVertically { it } + fadeOut(tween(180))
        ) {
            PlannerEditorScreen(
                item      = state.editorPlanItem,
                date      = state.selectedDate,
                isSaving  = state.isSaving,
                onSave    = vm::savePlanItem,
                onDismiss = vm::closePlanEditor
            )
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        state.errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp).clickable { vm.clearError() })
                    }
                }
            }
        }
    }
}

// ── NOTES tab ─────────────────────────────────────────────────────────────────

private fun LazyListScope.notesTabItems(state: CanvasUiState, vm: CanvasViewModel) {
    // Hero card (same as existing UI)
    item(key = "hero") {
        CanvasHeroCard(noteCount = state.notes.size)
    }

    if (state.isLoading) {
        item(key = "notes_loading") {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandEnd, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    // Weekly stats (computed from real notes)
    val pinned  = state.notes.count { it.isPinned }
    val today   = state.notes.count { it.updatedAt.startsWith(java.time.LocalDate.now().toString()) }
    item(key = "stats") {
        NoteStatsRow(todayCount = today, pinnedCount = pinned, totalCount = state.notes.size)
    }

    // Boards summary
    item(key = "boards") {
        BoardsSection(state.notes)
    }

    // Pinned notes
    val pinnedNotes = state.notes.filter { it.isPinned }
    if (pinnedNotes.isNotEmpty()) {
        item(key = "pinned_header") {
            SectionTitle("Pinned")
        }
        items(pinnedNotes, key = { "pin_${it.id}" }) { note ->
            NoteCard(note, vm)
        }
    }

    // All notes
    val unpinned = state.notes.filter { !it.isPinned }
    if (unpinned.isNotEmpty()) {
        item(key = "all_header") {
            SectionTitle(if (pinnedNotes.isEmpty()) "Your notes" else "All notes")
        }
        items(unpinned, key = { "note_${it.id}" }) { note ->
            NoteCard(note, vm)
        }
    }

    if (state.notes.isEmpty()) {
        item(key = "empty") {
            EmptyNotesCard { vm.openNewNote() }
        }
    }
}

// ── PLANNER tab ───────────────────────────────────────────────────────────────

private fun LazyListScope.plannerTabItems(state: CanvasUiState, vm: CanvasViewModel) {
    item(key = "planner_header") {
        PlannerHeaderCard(
            selectedDate = state.selectedDate,
            onDateSelected = vm::onDateSelected
        )
    }

    val itemsForDay = state.plannerItems.filter { it.date == state.selectedDate }

    if (itemsForDay.isEmpty()) {
        item(key = "planner_empty") {
            EmptyPlannerCard(state.selectedDate) { vm.openNewPlanItem() }
        }
    } else {
        items(itemsForDay, key = { "plan_${it.id}" }) { item ->
            PlannerItemCard(
                item     = item,
                onToggle = { vm.togglePlanDone(item) },
                onEdit   = { vm.openEditPlanItem(item) },
                onDelete = { vm.deletePlanItem(item) }
            )
        }
    }
}

// ── POSTED tab ────────────────────────────────────────────────────────────────

private fun LazyListScope.postedTabItems(state: CanvasUiState, vm: CanvasViewModel) {
    val postedNotes = state.notes.filter { it.isPublic }
    if (postedNotes.isEmpty()) {
        item(key = "posted_empty") {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Public, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Text("No posted notes yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Make a note public from the editor to share it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        item(key = "posted_title") { SectionTitle("Community notes") }
        items(postedNotes, key = { "pub_${it.id}" }) { note ->
            PostedNoteCard(note)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header + Tabs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CanvasHeader() {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Canvas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("notes, planner, and posted thoughts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassBtn(Icons.Rounded.Search, "Search") {}
            GlassBtn(Icons.Rounded.NotificationsNone, "Notifications") {}
        }
    }
}

@Composable
private fun CanvasTopTabRow(selected: CanvasTopTab, onSelect: (CanvasTopTab) -> Unit) {
    Surface(
        modifier        = Modifier.padding(horizontal = 20.dp),
        shape           = RoundedCornerShape(22.dp),
        color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        tonalElevation  = 0.dp,
        shadowElevation = 18.dp,
        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CanvasTopTab.entries.forEach { tab ->
                val sel = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (sel) ConektGradient.brandHorizontal
                            else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)))
                        )
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Notes tab components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CanvasHeroCard(noteCount: Int) {
    Card(
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.12f), BrandEnd.copy(alpha = 0.08f), MaterialTheme.colorScheme.surface)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Good ${greeting()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "You have $noteCount ${if (noteCount == 1) "note" else "notes"} ✍️",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(Modifier.size(42.dp), CircleShape, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CalendarMonth, "Calendar", tint = BrandEnd)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteStatsRow(todayCount: Int, pinnedCount: Int, totalCount: Int) {
    val stats = listOf(
        Triple("Today",  todayCount.toString(),  Color(0xFFB9E86C)),
        Triple("Pinned", pinnedCount.toString(), Color(0xFFFF8B6E)),
        Triple("Total",  totalCount.toString(),  Color(0xFFF2D458))
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.forEach { (label, value, accent) ->
            Card(
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(containerColor = accent),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun BoardsSection(notes: List<NoteItem>) {
    val boards = NoteBoard.entries.map { board ->
        val count = notes.count { it.board == board }
        Triple(board, count, boardAccent(board))
    }
    Column {
        SectionTitle("Boards")
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(boards, key = { it.first.name }) { (board, count, accent) ->
                Card(
                    modifier  = Modifier.width(220.dp).wrapContentHeight(),
                    shape     = RoundedCornerShape(28.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), MaterialTheme.colorScheme.surface)))
                            .padding(18.dp)
                    ) {
                        Column {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)) {
                                Text(board.tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(board.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 14.dp))
                            Text("$count ${if (count == 1) "note" else "notes"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(3) { Box(Modifier.size(10.dp).clip(CircleShape).background(accent.copy(alpha = if (it == 0) 1f else 0.5f - it * 0.15f))) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteItem, vm: CanvasViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    val accent = try { Color(android.graphics.Color.parseColor(note.coverColor)) } catch (_: Exception) { BrandEnd }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { vm.openEditNote(note) },
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.12f), MaterialTheme.colorScheme.surface)))
        ) {
            // Color accent strip on left
            Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)).background(accent))

            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.14f)) {
                                Text(note.board.tag, style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            if (note.isPinned) {
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFD86B).copy(alpha = 0.18f)) {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.PushPin, null, tint = Color(0xFFFFD86B), modifier = Modifier.size(10.dp))
                                        Text("pinned", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD86B))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(note.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box {
                        GlassBtn(Icons.Rounded.MoreHoriz, "More") { showMenu = true }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text(if (note.isPinned) "Unpin" else "Pin") }, onClick = { showMenu = false; vm.togglePin(note) }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; vm.openEditNote(note) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; vm.deleteNote(note) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) })
                        }
                    }
                }
                if (note.body.isNotBlank()) {
                    Text(note.body.take(120), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatRelativeTime(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("${note.wordCount}w", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PostedNoteCard(note: NoteItem) {
    val accent = try { Color(android.graphics.Color.parseColor(note.coverColor)) } catch (_: Exception) { BrandEnd }
    Card(
        shape     = RoundedCornerShape(30.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.EditNote, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(note.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(note.board.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.12f)) {
                    Text(note.board.tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
            Text(note.title,  style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 16.dp))
            Text(note.body.take(200), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Text(formatRelativeTime(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
            Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill(Icons.Rounded.FavoriteBorder, "Like")
                ActionPill(Icons.Rounded.ChatBubbleOutline, "Comment")
                ActionPill(Icons.Rounded.Send, "Share")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Planner tab components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlannerHeaderCard(selectedDate: String, onDateSelected: (String) -> Unit) {
    val today    = LocalDate.now()
    val selected = runCatching { LocalDate.parse(selectedDate) }.getOrDefault(today)
    val dates    = (-1..5).map { today.plusDays(it.toLong()) }
    val dayFmt   = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dateFmt  = DateTimeFormatter.ofPattern("d",   Locale.getDefault())

    Card(
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(InfoBlue.copy(alpha = 0.12f), BrandEnd.copy(alpha = 0.08f), MaterialTheme.colorScheme.surface)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = BrandEnd
                        )
                        Text("Select a day to view tasks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.forEach { date ->
                        val isSel = date == selected
                        val isToday = date == today
                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isSel) Brush.verticalGradient(listOf(BrandStart, BrandEnd))
                                    else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)))
                                )
                                .border(1.dp, if (isToday && !isSel) BrandEnd.copy(alpha = 0.40f) else Color.Transparent, RoundedCornerShape(24.dp))
                                .clickable { onDateSelected(dateStr) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.format(dayFmt).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(date.format(dateFmt), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                if (isToday) Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) Color.White else BrandEnd))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerItemCard(item: PlannerItem, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accent = try { Color(android.graphics.Color.parseColor(item.accentHex)) } catch (_: Exception) { BrandEnd }
    var showMenu by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Time column
        Column(Modifier.width(58.dp).padding(top = 10.dp), horizontalAlignment = Alignment.End) {
            Text(item.timeStart, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.timeEnd, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f))
        }
        Spacer(Modifier.width(10.dp))
        Card(
            modifier  = Modifier.weight(1f),
            shape     = RoundedCornerShape(22.dp),
            colors    = CardDefaults.cardColors(containerColor = if (item.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f) else MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(if (item.isDone) 2.dp else 6.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp)) {
                Box(Modifier.width(5.dp).height(52.dp).clip(RoundedCornerShape(99.dp)).background(if (item.isDone) accent.copy(alpha = 0.30f) else accent))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        item.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (item.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else null
                    )
                    if (item.body.isNotBlank()) {
                        Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp), maxLines = 2)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Done toggle
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(if (item.isDone) SuccessGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (item.isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Circle, null, tint = if (item.isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    Box {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).clickable { showMenu = true })
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) })
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Note Editor — full-screen rich editor
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorScreen(
    note:     NoteItem?,
    isSaving: Boolean,
    onSave:   (title: String, body: String, color: String, board: NoteBoard, pinned: Boolean, isPublic: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title      by rememberSaveable { mutableStateOf(note?.title ?: "") }
    var body       by rememberSaveable { mutableStateOf(note?.body  ?: "") }
    var coverColor by rememberSaveable { mutableStateOf(note?.coverColor ?: NoteColorPalette[0]) }
    var board      by rememberSaveable { mutableStateOf(note?.board ?: NoteBoard.PERSONAL) }
    var isPinned   by rememberSaveable { mutableStateOf(note?.isPinned ?: false) }
    var isPublic   by rememberSaveable { mutableStateOf(note?.isPublic ?: false) }

    // Editor formatting state
    var isBold       by rememberSaveable { mutableStateOf(false) }
    var isItalic     by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showBoardPicker by rememberSaveable { mutableStateOf(false) }

    val accent = try { Color(android.graphics.Color.parseColor(coverColor)) } catch (_: Exception) { BrandEnd }
    val wordCount = body.split("\\s+".toRegex()).count { it.isNotBlank() }
    val charCount = body.length

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background accent wash
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
        )

        Column(Modifier.fillMaxSize()) {
            // ── Top bar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Left: Back
                GlassBtn(Icons.Rounded.ArrowBack, "Back", onDismiss)

                // Center: Board selector
                Row(
                    modifier = Modifier.align(Alignment.Center).clickable { showBoardPicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.14f)) {
                        Text(board.tag, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }

                // Right: Save
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSaving) {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            } else {
                                Modifier.background(ConektGradient.brandHorizontal)
                            }
                        )
                        .clickable(enabled = !isSaving) { onSave(title, body, coverColor, board, isPinned, isPublic) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // ── Note content area ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Title field
                TextField(
                    value         = title,
                    onValueChange = { title = it },
                    placeholder   = {
                        Text("Note title…", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f), fontWeight = FontWeight.Bold)
                    },
                    textStyle     = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = accent,
                        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Meta row
                Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$wordCount words · $charCount chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    // Color dot
                    Box(Modifier.size(18.dp).clip(CircleShape).background(accent).border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), CircleShape).clickable { showColorPicker = !showColorPicker })
                    // Pin toggle
                    Icon(if (isPinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin, null,
                        tint = if (isPinned) Color(0xFFFFD86B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                        modifier = Modifier.size(18.dp).clickable { isPinned = !isPinned }
                    )
                    // Public toggle
                    Icon(if (isPublic) Icons.Rounded.Public else Icons.Rounded.Lock, null,
                        tint = if (isPublic) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                        modifier = Modifier.size(18.dp).clickable { isPublic = !isPublic }
                    )
                }

                // Color picker row
                AnimatedVisibility(showColorPicker) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        items(NoteColorPalette, key = { it }) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { BrandEnd }
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(c)
                                    .border(if (hex == coverColor) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { coverColor = hex; showColorPicker = false }
                            )
                        }
                    }
                }

                // Body field
                TextField(
                    value         = body,
                    onValueChange = { body = it },
                    placeholder   = { Text("Start writing your thoughts…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)) },
                    modifier      = Modifier.fillMaxWidth().defaultMinSize(minHeight = 360.dp),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = accent,
                        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                    ),
                    textStyle     = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(Modifier.height(40.dp))
            }

            // ── Formatting toolbar ────────────────────────────────────────────
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 16.dp,
                border          = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            ) {
                Column {
                    // Row 1: formatting
                    Row(
                        modifier              = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        FormatBtn("B", isBold, FontWeight.Bold) { isBold = !isBold; if (isBold) body += "**" }
                        FormatBtn("I", isItalic) { isItalic = !isItalic; if (isItalic) body += "_" }
                        FormatDivider()
                        FormatIconBtn(Icons.Rounded.FormatListBulleted) { body += "\n• " }
                        FormatIconBtn(Icons.Rounded.FormatListNumbered) { body += "\n1. " }
                        FormatIconBtn(Icons.Rounded.HorizontalRule)     { body += "\n---\n" }
                        FormatDivider()
                        FormatIconBtn(Icons.Rounded.Link)               { body += "[text](url)" }
                        FormatIconBtn(Icons.Rounded.Code)               { body += "`code`" }
                        FormatIconBtn(Icons.Rounded.FormatQuote)        { body += "\n> " }
                        Spacer(Modifier.weight(1f))
                        // Public toggle
                        Surface(
                            shape  = RoundedCornerShape(10.dp),
                            color  = if (isPublic) SuccessGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.clickable { isPublic = !isPublic }
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(if (isPublic) Icons.Rounded.Public else Icons.Rounded.Lock, null, tint = if (isPublic) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text(if (isPublic) "Public" else "Private", style = MaterialTheme.typography.labelSmall, color = if (isPublic) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // ── Board picker dialog ───────────────────────────────────────────────
        if (showBoardPicker) {
            AlertDialog(
                onDismissRequest = { showBoardPicker = false },
                title            = { Text("Select board") },
                text             = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoteBoard.entries.forEach { b ->
                            val ba = boardAccent(b)
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { board = b; showBoardPicker = false },
                                shape    = RoundedCornerShape(16.dp),
                                color    = if (b == board) ba.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                border   = if (b == board) BorderStroke(1.dp, ba.copy(alpha = 0.40f)) else null
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(ba))
                                    Column {
                                        Text(b.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(b.tag, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton    = {}
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Planner Item Editor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlannerEditorScreen(
    item:      PlannerItem?,
    date:      String,
    isSaving:  Boolean,
    onSave:    (PlannerItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title     by rememberSaveable { mutableStateOf(item?.title     ?: "") }
    var body      by rememberSaveable { mutableStateOf(item?.body      ?: "") }
    var selDate   by rememberSaveable { mutableStateOf(item?.date      ?: date) }
    var timeStart by rememberSaveable { mutableStateOf(item?.timeStart ?: "09:00") }
    var timeEnd   by rememberSaveable { mutableStateOf(item?.timeEnd   ?: "10:00") }
    var accentHex by rememberSaveable { mutableStateOf(item?.accentHex ?: NoteColorPalette[2]) }

    val accent     = try { Color(android.graphics.Color.parseColor(accentHex)) } catch (_: Exception) { BrandEnd }
    var showColors by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent))))

        Column(Modifier.fillMaxSize()) {
            // Top bar
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                GlassBtn(Icons.Rounded.ArrowBack, "Back", onDismiss)
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (item == null) "New task" else "Edit task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(selDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isSaving) {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            } else {
                                Modifier.background(ConektGradient.brandHorizontal)
                            }
                        )
                        .clickable(enabled = title.isNotBlank() && !isSaving) {
                            onSave(
                                PlannerItem(
                                    id        = item?.id ?: "",
                                    title     = title.trim(),
                                    body      = body.trim(),
                                    date      = selDate,
                                    timeStart = timeStart,
                                    timeEnd   = timeEnd,
                                    accentHex = accentHex,
                                    isDone    = item?.isDone ?: false
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Title
                EditorField("Task title", title, { title = it }, leadingIcon = Icons.Rounded.CheckCircle, singleLine = true, accent = accent)

                // Description
                EditorField("Description (optional)", body, { body = it }, leadingIcon = Icons.Rounded.Notes, singleLine = false, accent = accent)

                // Time range
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { TimeField("Start time", timeStart) { timeStart = it } }
                    Box(Modifier.weight(1f)) { TimeField("End time", timeEnd)     { timeEnd = it } }
                }

                // Color picker
                Column {
                    Text("Task color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(NoteColorPalette, key = { it }) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { BrandEnd }
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(c)
                                    .border(if (hex == accentHex) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { accentHex = hex }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit, leadingIcon: ImageVector, singleLine: Boolean, accent: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        TextField(
            value         = value,
            onValueChange = onChange,
            placeholder   = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon   = { Icon(leadingIcon, null, tint = accent, modifier = Modifier.size(20.dp)) },
            singleLine    = singleLine,
            minLines      = if (singleLine) 1 else 3,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(20.dp),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = accent
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        TextField(
            value         = value,
            onValueChange = { new ->
                // Enforce HH:MM format loosely
                val digits = new.filter { it.isDigit() }
                val formatted = when {
                    digits.length <= 2 -> digits
                    digits.length <= 4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
                    else               -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
                }
                onChange(formatted)
            },
            placeholder   = { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon   = { Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(18.dp)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(20.dp),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyNotesCard(onCreate: () -> Unit) {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(6.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandStart.copy(alpha = 0.10f), BrandEnd.copy(alpha = 0.06f), MaterialTheme.colorScheme.surface)))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(22.dp)).background(ConektGradient.brandHorizontal), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.EditNote, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text("No notes yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 16.dp))
                Text("Capture your first idea or thought", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                Box(
                    modifier = Modifier.padding(top = 20.dp).clip(RoundedCornerShape(18.dp))
                        .background(ConektGradient.brandHorizontal).clickable { onCreate() }.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Write your first note", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlannerCard(date: String, onCreate: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Text("Nothing planned for $date", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Tap + to add your first task for the day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CanvasFab(selectedTab: CanvasTopTab, onNewNote: () -> Unit, onNewPlan: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, shadowElevation = 18.dp) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(ConektGradient.brandHorizontal)
                .clickable { if (selectedTab == CanvasTopTab.PLANNER) onNewPlan() else onNewNote() },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (selectedTab == CanvasTopTab.PLANNER) Icons.Rounded.CalendarMonth else Icons.Rounded.EditNote, "New", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Formatting toolbar helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormatBtn(text: String, active: Boolean, fontWeight: FontWeight = FontWeight.Normal, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) BrandEnd.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = fontWeight), color = if (active) BrandEnd else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FormatIconBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FormatDivider() {
    Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)))
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared micro components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionPill(icon: ImageVector, label: String) {
    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GlassBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        Modifier.size(40.dp), CircleShape,
        MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Box(Modifier.clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun boardAccent(board: NoteBoard): Color = when (board) {
    NoteBoard.PERSONAL -> BrandEnd
    NoteBoard.WORK     -> InfoBlue
    NoteBoard.IDEAS    -> SuccessGreen
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else      -> "evening"
    }
}

private fun formatRelativeTime(iso: String): String {
    return try {
        val then = java.time.Instant.parse(iso)
        val diff = java.time.Duration.between(then, java.time.Instant.now())
        when {
            diff.toMinutes() < 1  -> "just now"
            diff.toHours()   < 1  -> "${diff.toMinutes()}m ago"
            diff.toDays()    < 1  -> "${diff.toHours()}h ago"
            diff.toDays()    < 7  -> "${diff.toDays()}d ago"
            else                  -> "${diff.toDays() / 7}w ago"
        }
    } catch (_: Exception) { "" }
}
