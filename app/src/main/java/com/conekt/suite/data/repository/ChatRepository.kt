package com.conekt.suite.data.repository

import android.content.Context
import android.net.Uri
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.feature.chat.ConversationItem
import com.conekt.suite.feature.chat.FullUserProfile
import com.conekt.suite.feature.chat.MessageItem
import com.conekt.suite.feature.chat.StoryPreview
import com.conekt.suite.feature.chat.UserSearchResult
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

// ─────────────────────────────────────────────────────────────────────────────
// Row DTOs — must be file-level for serialization plugin
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
private data class ConvRow(
    val id: String                                        = "",
    val type: String                                      = "direct",
    val name: String?                                     = null,
    @SerialName("avatar_url")      val avatarUrl: String? = null,
    @SerialName("last_message")    val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null
)

@Serializable
private data class MemberConvRow(
    @SerialName("conversation_id") val conversationId: String
)

@Serializable
private data class MemberRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id")         val userId: String
)

@Serializable
private data class MsgRow(
    val id: String                                       = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id")       val senderId: String        = "",
    val body: String?                                    = null,
    @SerialName("message_type")    val messageType: String     = "text",
    @SerialName("file_url")        val fileUrl: String?        = null,
    @SerialName("file_name")       val fileName: String?       = null,
    @SerialName("is_deleted")      val isDeleted: Boolean      = false,
    @SerialName("created_at")      val createdAt: String       = ""
)

@Serializable
private data class MsgInsert(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id")       val senderId: String,
    val body: String?                                    = null,
    @SerialName("message_type")    val messageType: String = "text",
    @SerialName("file_url")        val fileUrl: String?   = null,
    @SerialName("file_name")       val fileName: String?  = null
)

@Serializable
private data class ProfileRow(
    val id: String,
    val username: String,
    @SerialName("display_name")     val displayName: String?  = null,
    @SerialName("avatar_url")       val avatarUrl: String?    = null,
    @SerialName("banner_url")       val bannerUrl: String?    = null,
    val bio: String?                                          = null,
    @SerialName("is_verified")      val isVerified: Boolean   = false,
    @SerialName("is_private")       val isPrivate: Boolean    = false,
    @SerialName("follower_count")   val followerCount: Int    = 0,
    @SerialName("following_count")  val followingCount: Int   = 0
)

@Serializable
private data class StoryAuthorRow(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String?   = null
)

@Serializable
private data class StoryRow(
    val id: String,
    @SerialName("author_id")  val authorId: String,
    @SerialName("media_url")  val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "image",
    val caption: String?                              = null,
    @SerialName("expires_at") val expiresAt: String   = "",
    val author: StoryAuthorRow
)

@Serializable
private data class FollowCheckRow(
    @SerialName("follower_id")  val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
private data class ConvInsertRow(
    val type: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
private data class MemberInsertRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id")         val userId: String
)

@Serializable
private data class ConvUpdateRow(
    @SerialName("last_message")    val lastMessage: String,
    @SerialName("last_message_at") val lastMessageAt: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Encryption — AES-256-CBC, key = SHA-256(sort(uid1+uid2))
// ─────────────────────────────────────────────────────────────────────────────

private object Crypto {
    private const val ALG = "AES/CBC/PKCS5Padding"

    private fun key(a: String, b: String): ByteArray {
        val sorted = listOf(a, b).sorted().joinToString("|")
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(sorted.toByteArray())
            .copyOf(32)
    }

    fun encrypt(plain: String, a: String, b: String): String = runCatching {
        val k = SecretKeySpec(key(a, b), "AES")
        val c = Cipher.getInstance(ALG).also { it.init(Cipher.ENCRYPT_MODE, k) }
        val enc = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(c.iv + enc)
    }.getOrDefault(plain)

    fun decrypt(cipher: String, a: String, b: String): String = runCatching {
        val k    = SecretKeySpec(key(a, b), "AES")
        val raw  = Base64.getDecoder().decode(cipher)
        val iv   = raw.copyOfRange(0, 16)
        val data = raw.copyOfRange(16, raw.size)
        val c    = Cipher.getInstance(ALG).also { it.init(Cipher.DECRYPT_MODE, k, IvParameterSpec(iv)) }
        String(c.doFinal(data), Charsets.UTF_8)
    }.getOrDefault("[encrypted]")
}

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────

class ChatRepository(
    private val auth: AuthRepository = AuthRepository()
) {
    private val db = SupabaseProvider.client
    private fun me() = auth.currentUserId() ?: error("Not signed in")
    private fun now() = java.time.Instant.now().toString()

    // ── Conversations ─────────────────────────────────────────────────────────

    suspend fun fetchConversations(): List<ConversationItem> {
        val uid = me()
        // 1. Get conversation IDs I'm a member of
        val myConvIds = runCatching {
            db.from("conversation_members")
                .select(Columns.raw("conversation_id")) { filter { eq("user_id", uid) } }
                .decodeList<MemberConvRow>()
                .map { it.conversationId }
        }.getOrDefault(emptyList())

        if (myConvIds.isEmpty()) return emptyList()

        // 2. For each conv, find the other member and load conversation row
        return runCatching {
            myConvIds.mapNotNull { convId ->
                val conv = runCatching {
                    db.from("conversations")
                        .select { filter { eq("id", convId) } }
                        .decodeSingle<ConvRow>()
                }.getOrNull() ?: return@mapNotNull null

                // Find the other user in this conversation
                val otherMember = runCatching {
                    db.from("conversation_members")
                        .select { filter { eq("conversation_id", convId); neq("user_id", uid) } }
                        .decodeList<MemberRow>()
                        .firstOrNull()
                }.getOrNull()

                val otherUserId = otherMember?.userId ?: ""

                // Load their profile for name/avatar
                val otherProfile = if (otherUserId.isNotBlank()) {
                    runCatching {
                        db.from("profiles")
                            .select { filter { eq("id", otherUserId) } }
                            .decodeSingle<ProfileRow>()
                    }.getOrNull()
                } else null

                ConversationItem(
                    id            = conv.id,
                    type          = conv.type,
                    name          = otherProfile?.displayName ?: otherProfile?.username ?: conv.name,
                    avatarUrl     = otherProfile?.avatarUrl ?: conv.avatarUrl,
                    lastMessage   = conv.lastMessage,
                    lastMessageAt = conv.lastMessageAt,
                    otherUserId   = otherUserId
                )
            }.sortedByDescending { it.lastMessageAt }
        }.getOrDefault(emptyList())
    }

    suspend fun getOrCreateDirectConversation(otherUserId: String): String {
        val uid = me()
        // Find existing shared conversation
        return runCatching {
            val mine  = db.from("conversation_members")
                .select(Columns.raw("conversation_id")) { filter { eq("user_id", uid) } }
                .decodeList<MemberConvRow>().map { it.conversationId }.toSet()
            val theirs = db.from("conversation_members")
                .select(Columns.raw("conversation_id")) { filter { eq("user_id", otherUserId) } }
                .decodeList<MemberConvRow>().map { it.conversationId }.toSet()

            val shared = mine.intersect(theirs)
            if (shared.isNotEmpty()) return shared.first()

            // Create new
            val conv = db.from("conversations")
                .insert(ConvInsertRow(type = "direct", createdBy = uid)) { select() }
                .decodeSingle<ConvRow>()
            db.from("conversation_members").insert(listOf(
                MemberInsertRow(conv.id, uid),
                MemberInsertRow(conv.id, otherUserId)
            ))
            conv.id
        }.getOrDefault("")
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    suspend fun fetchMessages(convId: String, otherUserId: String): List<MessageItem> {
        val uid = me()
        return runCatching {
            db.from("messages")
                .select {
                    filter { eq("conversation_id", convId); eq("is_deleted", false) }
                    order("created_at", Order.ASCENDING)
                    limit(100)
                }
                .decodeList<MsgRow>()
                .map { r ->
                    val decBody = if (r.messageType == "text" && r.body != null)
                        Crypto.decrypt(r.body, uid, otherUserId)
                    else r.body
                    MessageItem(
                        id             = r.id,
                        conversationId = r.conversationId,
                        senderId       = r.senderId,
                        body           = decBody,
                        messageType    = r.messageType,
                        fileUrl        = r.fileUrl,
                        fileName       = r.fileName,
                        isMe           = r.senderId == uid,
                        createdAt      = r.createdAt,
                        isDeleted      = r.isDeleted
                    )
                }
        }.getOrDefault(emptyList())
    }

    suspend fun sendText(convId: String, text: String, otherUserId: String) {
        val uid = me()
        val enc = Crypto.encrypt(text, uid, otherUserId)
        db.from("messages").insert(MsgInsert(convId, uid, body = enc, messageType = "text"))
        db.from("conversations").update(ConvUpdateRow(text.take(60), now())) {
            filter { eq("id", convId) }
        }
    }

    suspend fun sendFile(context: Context, convId: String, uri: Uri, otherUserId: String) {
        val uid      = me()
        val mime     = context.contentResolver.getType(uri) ?: ""
        val name     = uri.lastPathSegment ?: "file"
        val bytes    = context.contentResolver.openInputStream(uri)?.readBytes() ?: return
        val path     = "$uid/chat/${System.currentTimeMillis()}_$name"

        db.storage.from("conekt-files").upload(path, bytes) { upsert = true }
        val url = db.storage.from("conekt-files").publicUrl(path)

        val msgType = when {
            mime.startsWith("image") -> "image"
            mime.startsWith("audio") -> "audio"
            else -> "file"
        }
        db.from("messages").insert(MsgInsert(convId, uid, messageType = msgType, fileUrl = url, fileName = name))
        db.from("conversations").update(ConvUpdateRow("📎 $name", now())) {
            filter { eq("id", convId) }
        }
    }

    suspend fun sendMusicTrack(convId: String, trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        val uid  = me()
        val body = """{"trackId":"$trackId","title":"${title.replace("\"","'")}","artist":"${artist.replace("\"","'")}","coverUrl":"${coverUrl.orEmpty()}","fileUrl":"$fileUrl"}"""
        db.from("messages").insert(MsgInsert(convId, uid, body = body, messageType = "music"))
        db.from("conversations").update(ConvUpdateRow("🎵 $title", now())) {
            filter { eq("id", convId) }
        }
    }

    suspend fun deleteMessage(messageId: String) {
        db.from("messages").update({ set("is_deleted", true) }) {
            filter { eq("id", messageId) }
        }
    }

    // ── Stories ───────────────────────────────────────────────────────────────

    suspend fun fetchStories(): List<StoryPreview> {
        val cols = Columns.raw("""
            id, author_id, media_url, media_type, caption, expires_at,
            author:profiles!stories_author_id_fkey(id, username, display_name, avatar_url)
        """.trimIndent())
        return runCatching {
            db.from("stories")
                .select(cols) {
                    filter { gt("expires_at", now()) }
                    order("created_at", Order.DESCENDING)
                    limit(30)
                }
                .decodeList<StoryRow>()
                .map { r ->
                    StoryPreview(
                        id         = r.id,
                        authorId   = r.authorId,
                        authorName = r.author.displayName ?: r.author.username,
                        avatarUrl  = r.author.avatarUrl,
                        mediaUrl   = r.mediaUrl,
                        caption    = r.caption
                    )
                }
        }.getOrDefault(emptyList())
    }

    // ── People search ─────────────────────────────────────────────────────────

    suspend fun searchUsers(q: String): List<UserSearchResult> = runCatching {
        db.from("profiles")
            .select {
                filter {
                    or {
                        ilike("username",     "%$q%")
                        ilike("display_name", "%$q%")
                    }
                }
                limit(20)
            }
            .decodeList<ProfileRow>()
            .map { r ->
                UserSearchResult(r.id, r.username, r.displayName, r.avatarUrl, r.bio)
            }
    }.getOrDefault(emptyList())

    suspend fun fetchUserProfile(userId: String): FullUserProfile? = runCatching {
        val uid = me()
        val r = db.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileRow>()
        val following = runCatching {
            db.from("follows")
                .select { filter { eq("follower_id", uid); eq("following_id", userId) } }
                .decodeList<FollowCheckRow>()
                .isNotEmpty()
        }.getOrDefault(false)
        FullUserProfile(r.id, r.username, r.displayName, r.avatarUrl, r.bannerUrl, r.bio, r.isVerified, r.isPrivate, r.followerCount, r.followingCount, following)
    }.getOrNull()

    suspend fun followUser(userId: String) {
        db.from("follows").upsert(
            FollowCheckRow(followerId = me(), followingId = userId)
        )
    }

    suspend fun unfollowUser(userId: String) {
        db.from("follows").delete {
            filter { eq("follower_id", me()); eq("following_id", userId) }
        }
    }
}