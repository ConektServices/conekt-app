package com.conekt.suite.feature.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.CanvasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CanvasViewModel(
    private val repo: CanvasRepository = CanvasRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CanvasUiState())
    val state: StateFlow<CanvasUiState> = _state.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        val today = LocalDate.now().format(dateFormatter)
        update { copy(selectedDate = today) }
        loadNotes()
        loadPlannerForDate(today)
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadNotes() {
        viewModelScope.launch {
            update { copy(isLoading = true, errorMessage = null) }
            val notes = safe { repo.fetchNotes() } ?: emptyList()
            update { copy(isLoading = false, notes = notes) }
        }
    }

    fun loadPlannerForDate(date: String) {
        viewModelScope.launch {
            val items = safe { repo.fetchPlannerItemsForDate(date) } ?: emptyList()
            update { copy(plannerItems = items, selectedDate = date) }
        }
    }

    fun refresh() {
        loadNotes()
        loadPlannerForDate(_state.value.selectedDate)
    }

    // ── Note editor ───────────────────────────────────────────────────────────

    fun openNewNote() {
        update { copy(editorNote = null, showEditor = true) }
    }

    fun openEditNote(note: NoteItem) {
        update { copy(editorNote = note, showEditor = true) }
    }

    fun closeEditor() {
        update { copy(showEditor = false, editorNote = null, saveSuccess = false) }
    }

    fun saveNote(
        title:      String,
        body:       String,
        coverColor: String,
        board:      NoteBoard,
        isPinned:   Boolean,
        isPublic:   Boolean
    ) {
        viewModelScope.launch {
            update { copy(isSaving = true, errorMessage = null) }
            val existing = _state.value.editorNote
            val result = if (existing == null) {
                safe { repo.createNote(title, body, coverColor, board, isPinned, isPublic) }
            } else {
                safe { repo.updateNote(existing.id, title, body, coverColor, board, isPinned, isPublic) }
            }
            if (result != null) {
                loadNotes()
                update { copy(isSaving = false, saveSuccess = true, showEditor = false, editorNote = null) }
            } else {
                update { copy(isSaving = false, errorMessage = "Failed to save note.") }
            }
        }
    }

    fun togglePin(note: NoteItem) {
        viewModelScope.launch {
            safe { repo.togglePin(note.id, !note.isPinned) }
            loadNotes()
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            safe { repo.deleteNote(note.id) }
            loadNotes()
        }
    }

    // ── Planner editor ────────────────────────────────────────────────────────

    fun openNewPlanItem() {
        update { copy(editorPlanItem = null, showPlanEditor = true) }
    }

    fun openEditPlanItem(item: PlannerItem) {
        update { copy(editorPlanItem = item, showPlanEditor = true) }
    }

    fun closePlanEditor() {
        update { copy(showPlanEditor = false, editorPlanItem = null, saveSuccess = false) }
    }

    fun savePlanItem(item: PlannerItem) {
        viewModelScope.launch {
            update { copy(isSaving = true) }
            val existing = _state.value.editorPlanItem
            val result = if (existing == null) {
                safe { repo.createPlannerItem(item) }
            } else {
                safe { repo.updatePlannerItem(item.copy(id = existing.id)) }
            }
            if (result != null) {
                loadPlannerForDate(_state.value.selectedDate)
                update { copy(isSaving = false, showPlanEditor = false, editorPlanItem = null) }
            } else {
                update { copy(isSaving = false, errorMessage = "Failed to save plan item.") }
            }
        }
    }

    fun togglePlanDone(item: PlannerItem) {
        viewModelScope.launch {
            safe { repo.togglePlannerDone(item.id, !item.isDone) }
            loadPlannerForDate(_state.value.selectedDate)
        }
    }

    fun deletePlanItem(item: PlannerItem) {
        viewModelScope.launch {
            safe { repo.deletePlannerItem(item.id) }
            loadPlannerForDate(_state.value.selectedDate)
        }
    }

    fun onDateSelected(date: String) = loadPlannerForDate(date)

    fun clearError() = update { copy(errorMessage = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun update(block: CanvasUiState.() -> CanvasUiState) {
        _state.value = _state.value.block()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? =
        try { block() } catch (e: Exception) { null }
}
