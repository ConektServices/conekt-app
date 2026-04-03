package com.conekt.suite.data.repository

import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.model.FilePreview
import com.conekt.suite.data.model.NotePreview
import com.conekt.suite.data.model.PostWithAuthor
import com.conekt.suite.data.model.PulseHomeData
import com.conekt.suite.data.model.StoryWithAuthor
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class PulseRepository {

    private val supabase = SupabaseProvider.client

    suspend fun fetchHomeData(): PulseHomeData {
        val stories = runCatching { fetchStories() }.getOrDefault(emptyList())
        val posts = runCatching { fetchPosts() }.getOrDefault(emptyList())
        val recentFiles = runCatching { fetchRecentFiles() }.getOrDefault(emptyList())
        val recentNotes = runCatching { fetchRecentNotes() }.getOrDefault(emptyList())

        return PulseHomeData(
            stories = stories,
            posts = posts,
            recentFiles = recentFiles,
            recentNotes = recentNotes
        )
    }

    private suspend fun fetchStories(): List<StoryWithAuthor> {
        val columns = Columns.raw(
            """
            id,
            media_url,
            media_type,
            caption,
            created_at,
            author:profiles!stories_author_id_fkey (
                id,
                username,
                display_name,
                avatar_url
            )
            """.trimIndent()
        )

        return supabase
            .from("stories")
            .select(columns = columns) {
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = 12)
            }
            .decodeList<StoryWithAuthor>()
    }

    private suspend fun fetchPosts(): List<PostWithAuthor> {
        val columns = Columns.raw(
            """
            id,
            body,
            media_urls,
            post_type,
            visibility,
            like_count,
            comment_count,
            share_count,
            created_at,
            author:profiles!posts_author_id_fkey (
                id,
                username,
                display_name,
                avatar_url
            )
            """.trimIndent()
        )

        return supabase
            .from("posts")
            .select(columns = columns) {
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = 20)
            }
            .decodeList<PostWithAuthor>()
    }

    private suspend fun fetchRecentFiles(): List<FilePreview> {
        val columns = Columns.raw(
            """
            id,
            name,
            file_type,
            thumbnail_url,
            mime_type,
            size_bytes,
            created_at
            """.trimIndent()
        )

        return supabase
            .from("files")
            .select(columns = columns) {
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = 8)
            }
            .decodeList<FilePreview>()
    }

    private suspend fun fetchRecentNotes(): List<NotePreview> {
        val columns = Columns.raw(
            """
            id,
            title,
            body,
            cover_color,
            updated_at,
            is_pinned
            """.trimIndent()
        )

        return supabase
            .from("notes")
            .select(columns = columns) {
                order(column = "updated_at", order = Order.DESCENDING)
                limit(count = 4)
            }
            .decodeList<NotePreview>()
    }
}