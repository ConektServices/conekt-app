package com.conekt.suite.feature.pulse

import com.conekt.suite.data.model.FilePreview
import com.conekt.suite.data.model.NotePreview
import com.conekt.suite.data.model.PostWithAuthor
import com.conekt.suite.data.model.StoryWithAuthor

data class PulseUiState(
    val isLoading: Boolean = true,
    val stories: List<StoryWithAuthor> = emptyList(),
    val posts: List<PostWithAuthor> = emptyList(),
    val recentFiles: List<FilePreview> = emptyList(),
    val recentNotes: List<NotePreview> = emptyList(),
    val errorMessage: String? = null
)