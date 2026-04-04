package com.conekt.suite.data.repository

import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.feature.canvas.NoteBoard
import com.conekt.suite.feature.canvas.NoteItem
import com.conekt.suite.feature.canvas.PlannerItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Supabase row DTOs ─────────────────────────────────────────────────────────

@Serializable
private data class NoteRow(
    val id: String                              = "",
    @SerialName("owner_id")    val ownerId: String   = "",
    val title: String                           = "Untitled",
    val body: String                            = "",
    @SerialName("cover_color") val coverColor: String = "#FF7F2E",
    val tags: List<String>                      = emptyList(),
    @SerialName("is_pinned")   val isPinned: Boolean  = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_shared_post") val isSharedPost: Boolean = false,
    @SerialName("word_count")  val wordCount: Int     = 0,
    @SerialName("created_at")  val createdAt: String  = "",
    @SerialName("updated_at")  val updatedAt: String  = ""
)

@Serializable
private data class NoteInsert(
    @SerialName("owner_id")    val ownerId: String,
    val title: String,
    val body: String,
    @SerialName("cover_color") val coverColor: String,
    val tags: List<String>,
    @SerialName("is_pinned")   val isPinned: Boolean,
    @SerialName("is_shared_post") val isSharedPost: Boolean,
    @SerialName("word_count")  val wordCount: Int
)

@Serializable
private data class NoteUpdate(
    val title: String,
    val body: String,
    @SerialName("cover_color") val coverColor: String,
    val tags: List<String>,
    @SerialName("is_pinned")   val isPinned: Boolean,
    @SerialName("is_shared_post") val isSharedPost: Boolean,
    @SerialName("word_count")  val wordCount: Int,
    @SerialName("updated_at")  val updatedAt: String
)

@Serializable
private data class PlannerRow(
    val id: String                              = "",
    @SerialName("owner_id")    val ownerId: String   = "",
    val title: String                           = "",
    val body: String                            = "",
    val date: String                            = "",
    @SerialName("time_start")  val timeStart: String = "09:00",
    @SerialName("time_end")    val timeEnd: String   = "10:00",
    @SerialName("accent_hex")  val accentHex: String = "#FF7F2E",
    @SerialName("is_done")     val isDone: Boolean   = false,
    @SerialName("created_at")  val createdAt: String = "",
    @SerialName("updated_at")  val updatedAt: String = ""
)

@Serializable
private data class PlannerInsert(
    @SerialName("owner_id")    val ownerId: String,
    val title: String,
    val body: String,
    val date: String,
    @SerialName("time_start")  val timeStart: String,
    @SerialName("time_end")    val timeEnd: String,
    @SerialName("accent_hex")  val accentHex: String,
    @SerialName("is_done")     val isDone: Boolean
)

// ── Mapping helpers ───────────────────────────────────────────────────────────

private fun NoteRow.toDomain(): NoteItem {
    // board is derived from the first tag
    val board = NoteBoard.fromTag(tags.firstOrNull())
    return NoteItem(
        id         = id,
        title      = title,
        body       = body,
        board      = board,
        coverColor = coverColor,
        isPinned   = isPinned,
        isPublic   = isSharedPost,
        wordCount  = wordCount,
        updatedAt  = updatedAt,
        createdAt  = createdAt
    )
}

private fun PlannerRow.toDomain() = PlannerItem(
    id        = id,
    title     = title,
    body      = body,
    date      = date,
    timeStart = timeStart,
    timeEnd   = timeEnd,
    accentHex = accentHex,
    isDone    = isDone
)

// ── Repository ────────────────────────────────────────────────────────────────

class CanvasRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    private val supabase = SupabaseProvider.client
    private fun uid()   = authRepository.currentUserId() ?: error("Not authenticated")
    private fun now()   = java.time.Instant.now().toString()

    // ── Notes ─────────────────────────────────────────────────────────────────

    suspend fun fetchNotes(): List<NoteItem> =
        supabase.from("notes")
            .select {
                filter {
                    eq("owner_id", uid())
                    eq("is_archived", false)
                }
                order("is_pinned", order = Order.DESCENDING)
                order("updated_at", order = Order.DESCENDING)
            }
            .decodeList<NoteRow>()
            .map { it.toDomain() }

    suspend fun createNote(
        title:      String,
        body:       String,
        coverColor: String,
        board:      NoteBoard,
        isPinned:   Boolean,
        isPublic:   Boolean
    ): NoteItem =
        supabase.from("notes")
            .insert(
                NoteInsert(
                    ownerId    = uid(),
                    title      = title.ifBlank { "Untitled" },
                    body       = body,
                    coverColor = coverColor,
                    tags       = listOf(board.tag),
                    isPinned   = isPinned,
                    isSharedPost = isPublic,
                    wordCount  = body.split("\\s+".toRegex()).count { it.isNotBlank() }
                )
            ) { select() }
            .decodeSingle<NoteRow>()
            .toDomain()

    suspend fun updateNote(
        id:         String,
        title:      String,
        body:       String,
        coverColor: String,
        board:      NoteBoard,
        isPinned:   Boolean,
        isPublic:   Boolean
    ): NoteItem =
        supabase.from("notes")
            .update(
                NoteUpdate(
                    title        = title.ifBlank { "Untitled" },
                    body         = body,
                    coverColor   = coverColor,
                    tags         = listOf(board.tag),
                    isPinned     = isPinned,
                    isSharedPost = isPublic,
                    wordCount    = body.split("\\s+".toRegex()).count { it.isNotBlank() },
                    updatedAt    = now()
                )
            ) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<NoteRow>()
            .toDomain()

    suspend fun togglePin(id: String, pinned: Boolean) {
        supabase.from("notes")
            .update({ set("is_pinned", pinned); set("updated_at", now()) }) {
                filter { eq("id", id) }
            }
    }

    suspend fun deleteNote(id: String) {
        supabase.from("notes").update({ set("is_archived", true) }) {
            filter { eq("id", id) }
        }
    }

    // ── Planner ───────────────────────────────────────────────────────────────

    suspend fun fetchPlannerItems(dateFrom: String, dateTo: String): List<PlannerItem> =
        supabase.from("planner_items")
            .select {
                filter {
                    eq("owner_id", uid())
                    gte("date", dateFrom)
                    lte("date", dateTo)
                }
                order("date",       order = Order.ASCENDING)
                order("time_start", order = Order.ASCENDING)
            }
            .decodeList<PlannerRow>()
            .map { it.toDomain() }

    suspend fun fetchPlannerItemsForDate(date: String): List<PlannerItem> =
        supabase.from("planner_items")
            .select {
                filter {
                    eq("owner_id", uid())
                    eq("date", date)
                }
                order("time_start", order = Order.ASCENDING)
            }
            .decodeList<PlannerRow>()
            .map { it.toDomain() }

    suspend fun createPlannerItem(item: PlannerItem): PlannerItem =
        supabase.from("planner_items")
            .insert(
                PlannerInsert(
                    ownerId   = uid(),
                    title     = item.title,
                    body      = item.body,
                    date      = item.date,
                    timeStart = item.timeStart,
                    timeEnd   = item.timeEnd,
                    accentHex = item.accentHex,
                    isDone    = item.isDone
                )
            ) { select() }
            .decodeSingle<PlannerRow>()
            .toDomain()

    suspend fun updatePlannerItem(item: PlannerItem): PlannerItem =
        supabase.from("planner_items")
            .update({
                set("title",      item.title)
                set("body",       item.body)
                set("date",       item.date)
                set("time_start", item.timeStart)
                set("time_end",   item.timeEnd)
                set("accent_hex", item.accentHex)
                set("is_done",    item.isDone)
                set("updated_at", now())
            }) {
                select()
                filter { eq("id", item.id) }
            }
            .decodeSingle<PlannerRow>()
            .toDomain()

    suspend fun togglePlannerDone(id: String, done: Boolean) {
        supabase.from("planner_items")
            .update({ set("is_done", done); set("updated_at", now()) }) {
                filter { eq("id", id) }
            }
    }

    suspend fun deletePlannerItem(id: String) {
        supabase.from("planner_items").delete {
            filter { eq("id", id) }
        }
    }
}
