package com.conekt.suite.feature.canvas

import androidx.compose.ui.graphics.Color

// ── Board / Category ──────────────────────────────────────────────────────────

enum class NoteBoard(val label: String, val tag: String) {
    PERSONAL("Personal Space", "private"),
    WORK("Work Notes", "team"),
    IDEAS("Ideas Dump", "creative");

    companion object {
        fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag } ?: PERSONAL
    }
}

// ── Note visibility ───────────────────────────────────────────────────────────

enum class NoteVisibilityMode { PRIVATE, PUBLIC }

// ── Cover color palette ───────────────────────────────────────────────────────

val NoteColorPalette = listOf(
    "#FF7F2E", // BrandStart orange
    "#FF3C4D", // BrandEnd red
    "#4C8DFF", // InfoBlue
    "#46C171", // SuccessGreen
    "#FFB067", // SoftOrange
    "#FF7F9A", // SoftPink
    "#8B5CF6", // Purple
    "#57F2C4", // Teal
    "#F2D458", // Yellow
    "#B9E86C", // Lime
    "#2BD96B", // Green
    "#EC4899", // Pink
)

// ── Note domain model ─────────────────────────────────────────────────────────

data class NoteItem(
    val id:          String,
    val title:       String,
    val body:        String,
    val board:       NoteBoard,
    val coverColor:  String,     // hex
    val isPinned:    Boolean,
    val isPublic:    Boolean,
    val wordCount:   Int,
    val updatedAt:   String,     // ISO string
    val createdAt:   String
)

// ── Planner item ──────────────────────────────────────────────────────────────

data class PlannerItem(
    val id:        String,
    val title:     String,
    val body:      String,
    val date:      String,       // yyyy-MM-dd
    val timeStart: String,       // HH:mm  e.g. "09:00"
    val timeEnd:   String,       // HH:mm  e.g. "10:30"
    val accentHex: String,       // hex color
    val isDone:    Boolean
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class CanvasUiState(
    val isLoading:       Boolean              = true,
    val notes:           List<NoteItem>       = emptyList(),
    val plannerItems:    List<PlannerItem>    = emptyList(),
    val selectedDate:    String               = "",        // yyyy-MM-dd
    val errorMessage:    String?              = null,

    // Note editor
    val editorNote:      NoteItem?            = null,      // null = new note
    val showEditor:      Boolean              = false,

    // Planner editor
    val editorPlanItem:  PlannerItem?         = null,      // null = new item
    val showPlanEditor:  Boolean              = false,

    // Save state
    val isSaving:        Boolean              = false,
    val saveSuccess:     Boolean              = false
)
