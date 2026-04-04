package com.conekt.suite.data.repository

import android.content.Context
import android.net.Uri
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.feature.chat.ConversationItem
import com.conekt.suite.feature.chat.MessageItem
import com.conekt.suite.feature.chat.StoryItem
import com.conekt.suite.feature.chat.UserProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
private data class ConversationRow(
    val id: String,
    val type: String                                  = "direct",
    val name: String?                                 = null,
    @SerialName("avatar_url")     val avatarUrl: String? = null,
    @SerialName("last_message")   val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at")    val createdAt: String   = ""
)

@Serializable
private data class MessageRow(
    val id: String                                     = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id")       val senderId: String       = "",
    val body: String?                                  = null,
    @SerialName("message_type")    val messageType: String    = "text",
    @SerialName("file_url")        val fileUrl: String?       = null,
    @SerialName("file_name")       val fileName: String?      = null,
    @SerialName("is_deleted")      val isDeleted: Boolean     = false,
    @SerialName("created_at")      val createdAt: String      = ""
)

@Serializable
private data class MessageInsert(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id")       val senderId: String,
    val body: String?              = null,
    @SerialName("message_type")    val messageType: String = "text",
    @SerialName("file_url")        val fileUrl: String?   = null,
    @SerialName("file_name")       val fileName: String?  = null
)

@Serializable
private data class ProfileRow(
    val id: String,
    val username: String,
    @SerialName("display_name")     val displayName: String?   = null,
    @SerialName("avatar_url")       val avatarUrl: String?     = null,
    @SerialName("banner_url")       val bannerUrl: String?     = null,
    val bio: String?                                           = null,
    @SerialName("is_verified")      val isVerified: Boolean    = false,
    @SerialName("is_private")       val isPrivate: Boolean     = false,
    @SerialName("follower_count")   val followerCount: Int     = 0,
    @SerialName("following_count")  val followingCount: Int    = 0
)

@Serializable
private data class StoryRow(
    val id: String,
    @SerialName("author_id")  val authorId: String,
    @SerialName("media_url")  val mediaUrl: String,
    @SerialName("media_type") val mediaType: String,
    val caption: String?                       = null,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val author: ProfileSnippetRow
)

@Serializable
private data class ProfileSnippetRow(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String?   = null
)

@Serializable
private data class FollowRow(
    @SerialName("follower_id")  val followerId: String,
    @SerialName("following_id") val followingId: String
)

// ── Encryption helper ─────────────────────────────────────────────────────────
// Messages are encrypted with AES-256-CBC using a key derived from the sorted
// pair of user IDs (simple deterministic shared secret for demonstration).
// In production you would use Signal Protocol / ECDH key exchange.

private object MessageCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    private fun deriveKey(userId1: String, userId2: String): ByteArray {
        val sorted = listOf(userId1, userId2).sorted().joinToString("")
        val hash   = java.security.MessageDigest.getInstance("SHA-256").digest(sorted.toByteArray())
        return hash.copyOf(32) // 256-bit key
    }

    fun encrypt(plaintext: String, userId1: String, userId2: String): String {
        return try {
            val key    = deriveKey(userId1, userId2)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            val iv         = cipher.iv
            val encrypted  = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined   = iv + encrypted
            Base64.getEncoder().encodeToString(combined)
        } catch (_: Exception) { plaintext } // graceful fallback
    }

    fun decrypt(ciphertext: String, userId1: String, userId2: String): String {
        return try {
            val key      = deriveKey(userId1, userId2)
            val combined = Base64.getDecoder().decode(ciphertext)
            val iv       = combined.copyOfRange(0, 16)
            val data     = combined.copyOfRange(16, combined.size)
            val cipher   = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (_: Exception) { "[encrypted message]" } // if key mismatch
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class ChatRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    private val supabase = SupabaseProvider.client
    private fun uid()    = authRepository.currentUserId() ?: error("Not authenticated")

    // ── Conversations ─────────────────────────────────────────────────────────

    suspend fun fetchConversations(): List<ConversationItem> {
        val myUid = uid()
        return try {
            // Get conversation IDs the current user is a member of
            @Serializable data class MemberRow(
                @SerialName("conversation_id") val conversationId: String
            )
            val memberRows = supabase.from("conversation_members")
                .select(Columns.raw("conversation_id")) {
                    filter { eq("user_id", myUid) }
                }
                .decodeList<MemberRow>()

            val ids = memberRows.map { it.conversationId }
            if (ids.isEmpty()) return emptyList()

            supabase.from("conversations")
                .select {
                    filter { isIn("id", ids) }
                    order("last_message_at", order = Order.DESCENDING)
                }
                .decodeList<ConversationRow>()
                .map { row ->
                    // For direct chats, find the other user's name/avatar
                    ConversationItem(
                        id            = row.id,
                        type          = row.type,
                        name          = row.name,
                        avatarUrl     = row.avatarUrl,
                        lastMessage   = row.lastMessage,
                        lastMessageAt = row.lastMessageAt
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getOrCreateDirectConversation(otherUserId: String): String {
        val myUid = uid()
        // Check if a direct conversation already exists
        return try {
            @Serializable data class ConvIdRow(val id: String)
            @Serializable data class MemberConvRow(@SerialName("conversation_id") val conversationId: String)

            // Find conversations where both users are members
            val mine   = supabase.from("conversation_members")
                .select(Columns.raw("conversation_id")) { filter { eq("user_id", myUid) } }
                .decodeList<MemberConvRow>().map { it.conversationId }
            val theirs = supabase.from("conversation_members")
                .select(Columns.raw("conversation_id")) { filter { eq("user_id", otherUserId) } }
                .decodeList<MemberConvRow>().map { it.conversationId }

            val common = mine.intersect(theirs.toSet())
            if (common.isNotEmpty()) {
                return common.first()
            }

            // Create new conversation
            val conv = supabase.from("conversations")
                .insert(mapOf("type" to "direct", "created_by" to myUid)) { select() }
                .decodeSingle<ConversationRow>()

            // Add both members
            supabase.from("conversation_members").insert(
                listOf(
                    mapOf("conversation_id" to conv.id, "user_id" to myUid),
                    mapOf("conversation_id" to conv.id, "user_id" to otherUserId)
                )
            )
            conv.id
        } catch (_: Exception) { "" }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    suspend fun fetchMessages(conversationId: String, otherUserId: String): List<MessageItem> {
        val myUid = uid()
        return try {
            supabase.from("messages")
                .select {
                    filter { eq("conversation_id", conversationId); eq("is_deleted", false) }
                    order("created_at", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList<MessageRow>()
                .map { row ->
                    val decryptedBody = row.body?.let {
                        if (row.messageType == "text") {
                            MessageCrypto.decrypt(it, myUid, otherUserId)
                        } else it
                    }
                    MessageItem(
                        id             = row.id,
                        conversationId = row.conversationId,
                        senderId       = row.senderId,
                        body           = decryptedBody,
                        messageType    = row.messageType,
                        fileUrl        = row.fileUrl,
                        fileName       = row.fileName,
                        isMe           = row.senderId == myUid,
                        createdAt      = row.createdAt,
                        isDeleted      = row.isDeleted
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun sendTextMessage(conversationId: String, text: String, otherUserId: String) {
        val myUid   = uid()
        val encrypted = MessageCrypto.encrypt(text, myUid, otherUserId)
        supabase.from("messages").insert(
            MessageInsert(conversationId = conversationId, senderId = myUid, body = encrypted, messageType = "text")
        )
        // Update last_message on conversation
        supabase.from("conversations").update({
            set("last_message", text.take(60))
            set("last_message_at", java.time.Instant.now().toString())
        }) { filter { eq("id", conversationId) } }
    }

    suspend fun sendFileMessage(context: Context, conversationId: String, fileUri: Uri, otherUserId: String) {
        val myUid    = uid()
        val fileName = fileUri.lastPathSegment ?: "file"
        val bytes    = context.contentResolver.openInputStream(fileUri)?.readBytes() ?: return
        val path     = "$myUid/chat/${System.currentTimeMillis()}_$fileName"
        supabase.storage.from("conekt-files").upload(path, bytes) { upsert = true }
        val url = supabase.storage.from("conekt-files").publicUrl(path)
        val mimeType = context.contentResolver.getType(fileUri) ?: ""
        val msgType = when {
            mimeType.startsWith("image") -> "image"
            mimeType.startsWith("audio") -> "audio"
            else -> "file"
        }
        supabase.from("messages").insert(
            MessageInsert(conversationId = conversationId, senderId = myUid, body = null, messageType = msgType, fileUrl = url, fileName = fileName)
        )
        supabase.from("conversations").update({
            set("last_message", "📎 $fileName")
            set("last_message_at", java.time.Instant.now().toString())
        }) { filter { eq("id", conversationId) } }
    }

    suspend fun sendMusicMessage(conversationId: String, trackId: String, trackTitle: String, artist: String, coverUrl: String?, fileUrl: String) {
        val myUid = uid()
        val body  = """{"trackId":"$trackId","title":"$trackTitle","artist":"$artist","coverUrl":"${coverUrl.orEmpty()}","fileUrl":"$fileUrl"}"""
        supabase.from("messages").insert(
            MessageInsert(conversationId = conversationId, senderId = myUid, body = body, messageType = "music")
        )
        supabase.from("conversations").update({
            set("last_message", "🎵 $trackTitle")
            set("last_message_at", java.time.Instant.now().toString())
        }) { filter { eq("id", conversationId) } }
    }

    suspend fun deleteMessage(messageId: String) {
        supabase.from("messages").update({ set("is_deleted", true) }) { filter { eq("id", messageId) } }
    }

    // ── Stories ───────────────────────────────────────────────────────────────

    suspend fun fetchStories(): List<StoryItem> {
        val columns = Columns.raw("""
            id, author_id, media_url, media_type, caption, expires_at, created_at,
            author:profiles!stories_author_id_fkey(id, username, display_name, avatar_url)
        """.trimIndent())
        return try {
            supabase.from("stories")
                .select(columns) {
                    filter { gt("expires_at", java.time.Instant.now().toString()) }
                    order("created_at", order = Order.DESCENDING)
                    limit(30)
                }
                .decodeList<StoryRow>()
                .map { row ->
                    StoryItem(
                        id         = row.id,
                        authorId   = row.authorId,
                        authorName = row.author.displayName ?: row.author.username,
                        avatarUrl  = row.author.avatarUrl,
                        mediaUrl   = row.mediaUrl,
                        mediaType  = row.mediaType,
                        caption    = row.caption,
                        expiresAt  = row.expiresAt
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    // ── People search ─────────────────────────────────────────────────────────

    suspend fun searchUsers(query: String): List<UserProfile> {
        return try {
            supabase.from("profiles")
                .select {
                    filter {
                        or {
                            ilike("username",     "%$query%")
                            ilike("display_name", "%$query%")
                        }
                    }
                    limit(20)
                }
                .decodeList<ProfileRow>()
                .map { it.toUserProfile() }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchUserProfile(userId: String): UserProfile? {
        val myUid = uid()
        return try {
            val row = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<ProfileRow>()
            val isFollowing = try {
                supabase.from("follows").select {
                    filter { eq("follower_id", myUid); eq("following_id", userId) }
                }.decodeList<FollowRow>().isNotEmpty()
            } catch (_: Exception) { false }
            row.toUserProfile(isFollowing)
        } catch (_: Exception) { null }
    }

    suspend fun followUser(userId: String) {
        supabase.from("follows").upsert(FollowRow(followerId = uid(), followingId = userId))
    }

    suspend fun unfollowUser(userId: String) {
        supabase.from("follows").delete {
            filter { eq("follower_id", uid()); eq("following_id", userId) }
        }
    }

    private fun ProfileRow.toUserProfile(isFollowing: Boolean = false) = UserProfile(
        id             = id,
        username       = username,
        displayName    = displayName,
        avatarUrl      = avatarUrl,
        bannerUrl      = bannerUrl,
        bio            = bio,
        isVerified     = isVerified,
        isPrivate      = isPrivate,
        followerCount  = followerCount,
        followingCount = followingCount,
        isFollowing    = isFollowing
    )
}
