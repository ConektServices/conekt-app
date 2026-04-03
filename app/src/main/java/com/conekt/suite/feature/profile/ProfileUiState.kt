package com.conekt.suite.feature.profile

import com.conekt.suite.data.model.FileRecord
import com.conekt.suite.data.model.NoteRecord
import com.conekt.suite.data.model.PostRecord
import com.conekt.suite.data.model.ProfileRecord

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileRecord? = null,
    val posts: List<PostRecord> = emptyList(),
    val files: List<FileRecord> = emptyList(),
    val notes: List<NoteRecord> = emptyList(),
    val errorMessage: String? = null
)