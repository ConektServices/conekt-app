package com.conekt.suite.data.repository

import android.content.Context
import android.net.Uri
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.feature.chat.*
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

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
private data class ConvRow(
    val id: String = "",
    val type: String = "direct",
    val name: String? = null,
    @SerialName("avatar_url")      val avatarUrl: String? = null,
    @SerialName("last_message")    val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_by")      val createdBy: String? = null
)

@Serializable
private data class ConvInsertRow(
    val type: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
private data class ConvUpdateRow(
    @SerialName("last_message")    val lastMessage: String,
    @SerialName("last_message_at") val lastMessageAt: String
)

@Serializable
private data class MemberConvIdRow(
    @SerialName("conversation_id") val conversationId: String
)

@Serializable
private data class MemberRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id")         val userId: String
)

@Serializable
private data class MemberInsertRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id")         val userId: String
)

@Serializable
private data class MsgRow(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id")       val senderId: String = "",
    val body: String? = null,
    @SerialName("message_type")    val messageType: String = "text",
    @SerialName("file_url")        val fileUrl: String? = null,
    @SerialName("file_name")       val fileName: String? = null,
    @SerialName("is_deleted")      val isDeleted: Boolean = false,
    @SerialName("created_at")      val createdAt: String = ""
)

@Serializable
private data class MsgInsertRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id")       val senderId: String,
    val body: String? = null,
    @SerialName("message_type")    val messageType: String = "text",
    @SerialName("file_url")        val fileUrl: String? = null,
    @SerialName("file_name")       val fileName: String? = null
)

@Serializable
private data class ProfileRow(
    val id: String,
    val username: String,
    @SerialName("display_name")    val displayName: String? = null,
    @SerialName("avatar_url")      val avatarUrl: String? = null,
    @SerialName("banner_url")      val bannerUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_verified")     val isVerified: Boolean = false,
    @SerialName("follower_count")  val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0
)

@Serializable
private data class FollowRow(
    @SerialName("follower_id")  val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
private data class StoryAuthorRow(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String? = null
)

@Serializable
private data class StoryRow(
    val id: String,
    @SerialName("author_id")  val authorId: String,
    @SerialName("media_url")  val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "image",
    val caption: String? = null,
    @SerialName("expires_at") val expiresAt: String = "",
    val author: StoryAuthorRow
)

// ── AES-256-CBC Encryption ────────────────────────────────────────────────────
// Key is derived from SORTED user IDs so both parties get the same key.

private object MsgCrypto {
    private const val ALG = "AES/CBC/PKCS5Padding"

    private fun key(a: String, b: String): ByteArray {
        // Always sort so key(A,B) == key(B,A)
        val seed = listOf(a, b).sorted().joinToString("|")
        return java.security.MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
    }

    fun encrypt(plain: String, myUid: String, otherUid: String): String = runCatching {
        val key    = SecretKeySpec(key(myUid, otherUid), "AES")
        val cipher = Cipher.getInstance(ALG).also { it.init(Cipher.ENCRYPT_MODE, key) }
        val enc    = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(cipher.iv + enc)
    }.getOrDefault(plain)

    fun decrypt(cipherText: String, myUid: String, otherUid: String): String = runCatching {
        val raw  = Base64.getDecoder().decode(cipherText)
        val key  = SecretKeySpec(key(myUid, otherUid), "AES")
        val c    = Cipher.getInstance(ALG).also {
            it.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(raw.copyOfRange(0, 16)))
        }
        String(c.doFinal(raw.copyOfRange(16, raw.size)), Charsets.UTF_8)
    }.getOrDefault(cipherText) // fall back to raw if decryption fails
}

// ── Repository ────────────────────────────────────────────────────────────────

class ChatRepository(
    private val auth: AuthRepository = AuthRepository()
) {
    private val db  = SupabaseProvider.client
    private fun me()  = auth.currentUserId() ?: error("Not signed in")
    private fun now() = java.time.Instant.now().toString()

    // ── Conversations ─────────────────────────────────────────────────────────

    suspend fun fetchConversations(): List<ConversationItem> {
        val uid = me()

        // Get all conversation_ids I'm a member of
        val myMemberships = runCatching {
            db.from("conversation_members")
                .select(Columns.raw("conversation_id")) {
                    filter { eq("user_id", uid) }
                }
                .decodeList<MemberConvIdRow>()
                .map { it.conversationId }
        }.getOrDefault(emptyList())

        if (myMemberships.isEmpty()) return emptyList()

        return myMemberships.mapNotNull { convId ->
            runCatching {
                // Fetch conversation row
                val conv = db.from("conversations")
                    .select { filter { eq("id", convId) } }
                    .decodeList<ConvRow>()
                    .firstOrNull() ?: return@runCatching null

                // Fetch ALL members of this conversation
                // (now possible because the policy allows seeing other members
                //  of conversations you're in)
                val allMembers = db.from("conversation_members")
                    .select(Columns.raw("conversation_id, user_id")) {
                        filter { eq("conversation_id", convId) }
                    }
                    .decodeList<MemberRow>()

                // The other person is the member who is NOT me
                val otherUserId = allMembers
                    .firstOrNull { it.userId != uid }
                    ?.userId

                // Fetch the other person's profile
                val otherProfile = otherUserId?.let { otherId ->
                    runCatching {
                        db.from("profiles")
                            .select { filter { eq("id", otherId) } }
                            .decodeList<ProfileRow>()
                            .firstOrNull()
                    }.getOrNull()
                }

                ConversationItem(
                    id            = conv.id,
                    name          = otherProfile?.displayName
                        ?: otherProfile?.username
                        ?: conv.name
                        ?: "Chat",
                    avatarUrl     = otherProfile?.avatarUrl ?: conv.avatarUrl,
                    lastMessage   = conv.lastMessage,
                    lastMessageAt = conv.lastMessageAt,
                    otherUserId   = otherUserId ?: ""
                )
            }.getOrNull()
        }.sortedByDescending { it.lastMessageAt }
    }

    suspend fun getOrCreateDm(otherUserId: String): String {
        val uid = me()

        // Find my conversation IDs
        val mine = runCatching {
            db.from("conversation_members")
                .select(Columns.raw("conversation_id")) {
                    filter { eq("user_id", uid) }
                }
                .decodeList<MemberConvIdRow>()
                .map { it.conversationId }
                .toSet()
        }.getOrElse {
            android.util.Log.e("ChatRepo", "Failed to fetch my conversations: ${it.message}")
            emptySet()
        }

        // Find their conversation IDs
        val theirs = runCatching {
            db.from("conversation_members")
                .select(Columns.raw("conversation_id")) {
                    filter { eq("user_id", otherUserId) }
                }
                .decodeList<MemberConvIdRow>()
                .map { it.conversationId }
                .toSet()
        }.getOrElse {
            android.util.Log.e("ChatRepo", "Failed to fetch their conversations: ${it.message}")
            emptySet()
        }

        // Return existing shared conversation
        val shared = mine.intersect(theirs)
        if (shared.isNotEmpty()) {
            android.util.Log.d("ChatRepo", "Found existing DM: ${shared.first()}")
            return shared.first()
        }

        android.util.Log.d("ChatRepo", "Creating new DM between $uid and $otherUserId")

        // Insert conversation WITHOUT select() to avoid SELECT policy
        // firing before members are added
        db.from("conversations").insert(ConvInsertRow("direct", uid))

        // Fetch the conversation we just created
        val convId = db.from("conversations")
            .select {
                filter { eq("created_by", uid) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<ConvRow>()
            .firstOrNull()
            ?.id
            ?.also { android.util.Log.d("ChatRepo", "Got new convId: $it") }
            ?: error("Conversation was inserted but could not be fetched")

        // Add both members
        db.from("conversation_members").insert(
            listOf(
                MemberInsertRow(convId, uid),
                MemberInsertRow(convId, otherUserId)
            )
        )
        android.util.Log.d("ChatRepo", "Members added to $convId")

        return convId
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    suspend fun fetchMessages(convId: String, otherUserId: String): List<MessageItem> {
        val uid = me()

        // otherUserId is passed in from the navigation args — use it directly
        // for decryption so we always use the correct key pair
        val rows = db.from("messages")
            .select {
                filter {
                    eq("conversation_id", convId)
                    eq("is_deleted", false)
                }
                order("created_at", Order.ASCENDING)
                limit(200)
            }
            .decodeList<MsgRow>()

        return rows.map { r ->
            val type = when (r.messageType) {
                "image" -> MsgType.IMAGE
                "file"  -> MsgType.FILE
                "audio" -> MsgType.AUDIO
                "music" -> MsgType.MUSIC
                "emoji" -> MsgType.EMOJI
                else    -> MsgType.TEXT
            }

            // Determine the other party for this specific message
            // If I sent it: other = otherUserId
            // If they sent it: other = otherUserId (same — key is symmetric)
            val decryptedBody = when (type) {
                MsgType.TEXT, MsgType.EMOJI -> {
                    r.body?.let { MsgCrypto.decrypt(it, uid, otherUserId) }
                }
                MsgType.MUSIC -> r.body // music payloads are plain JSON
                else -> null            // images/files use fileUrl, not body
            }

            // Parse music JSON payload
            var mTitle: String? = null
            var mArtist: String? = null
            var mCover: String? = null
            var mFile: String? = null
            if (type == MsgType.MUSIC && r.body != null) {
                runCatching {
                    val j = kotlinx.serialization.json.Json
                        .parseToJsonElement(r.body)
                        .let { it as? kotlinx.serialization.json.JsonObject }
                    fun str(key: String) = j?.get(key)
                        ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                        ?.content
                    mTitle  = str("title")
                    mArtist = str("artist")
                    mCover  = str("coverUrl")?.ifBlank { null }
                    mFile   = str("fileUrl")
                }
            }

            MessageItem(
                id          = r.id,
                senderId    = r.senderId,
                body        = decryptedBody,
                type        = type,
                fileUrl     = r.fileUrl,
                fileName    = r.fileName,
                isMe        = r.senderId == uid,
                createdAt   = r.createdAt,
                isDeleted   = r.isDeleted,
                musicTitle  = mTitle,
                musicArtist = mArtist,
                musicCover  = mCover,
                musicFile   = mFile
            )
        }
    }

    suspend fun sendText(convId: String, text: String, otherUserId: String) {
        val uid = me()
        val isEmoji = text.trim().let { t ->
            t.length <= 2 && t.all { c ->
                !c.isLetterOrDigit() && !c.isWhitespace()
            }
        }
        val (msgType, body) = if (isEmoji) {
            "emoji" to text
        } else {
            "text" to MsgCrypto.encrypt(text, uid, otherUserId)
        }
        db.from("messages").insert(
            MsgInsertRow(convId, uid, body = body, messageType = msgType)
        )
        db.from("conversations").update(
            ConvUpdateRow(text.take(80), now())
        ) { filter { eq("id", convId) } }
    }

    suspend fun sendFile(context: Context, convId: String, uri: Uri, otherUserId: String) {
        val uid  = me()
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

        // Get a clean file name
        val rawName = uri.lastPathSegment
            ?: "file_${System.currentTimeMillis()}"
        val name = rawName.substringAfterLast("/").substringAfterLast(":")
            .ifBlank { "file_${System.currentTimeMillis()}" }

        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.readBytes()
            ?: error("Could not read file")

        val ext = when {
            mime.startsWith("image") -> ".jpg"
            mime.startsWith("audio") -> ".mp3"
            mime.startsWith("video") -> ".mp4"
            else -> ""
        }
        val path = "$uid/chat/${System.currentTimeMillis()}$ext"

        db.storage.from("conekt-files").upload(path, bytes) { upsert = true }
        val url = db.storage.from("conekt-files").publicUrl(path)

        val msgType = when {
            mime.startsWith("image") -> "image"
            mime.startsWith("audio") -> "audio"
            mime.startsWith("video") -> "image" // display like image for now
            else -> "file"
        }

        db.from("messages").insert(
            MsgInsertRow(
                conversationId = convId,
                senderId       = uid,
                messageType    = msgType,
                fileUrl        = url,
                fileName       = name
            )
        )
        val preview = when (msgType) {
            "image" -> "📷 Photo"
            "audio" -> "🎵 Audio"
            else    -> "📎 $name"
        }
        db.from("conversations").update(
            ConvUpdateRow(preview, now())
        ) { filter { eq("id", convId) } }
    }

    suspend fun sendMusic(
        convId: String,
        trackId: String,
        title: String,
        artist: String,
        coverUrl: String?,
        fileUrl: String
    ) {
        val uid  = me()
        val body = buildString {
            append("{")
            append("\"trackId\":\"$trackId\",")
            append("\"title\":\"${title.replace("\"", "'")}\",")
            append("\"artist\":\"${artist.replace("\"", "'")}\",")
            append("\"coverUrl\":\"${coverUrl.orEmpty()}\",")
            append("\"fileUrl\":\"$fileUrl\"")
            append("}")
        }
        db.from("messages").insert(
            MsgInsertRow(convId, uid, body = body, messageType = "music")
        )
        db.from("conversations").update(
            ConvUpdateRow("🎵 $title", now())
        ) { filter { eq("id", convId) } }
    }

    suspend fun deleteMessage(msgId: String) {
        db.from("messages")
            .update({ set("is_deleted", true) }) {
                filter { eq("id", msgId) }
            }
    }

    // ── Stories ───────────────────────────────────────────────────────────────

    suspend fun fetchStories(): List<StoryThumb> {
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
                    StoryThumb(
                        r.id, r.authorId,
                        r.author.displayName ?: r.author.username,
                        r.author.avatarUrl,
                        r.mediaUrl, r.caption
                    )
                }
        }.getOrDefault(emptyList())
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun searchUsers(q: String): List<UserSearchResult> = runCatching {
        db.from("profiles")
            .select {
                filter {
                    or {
                        ilike("username", "%$q%")
                        ilike("display_name", "%$q%")
                    }
                }
                limit(20)
            }
            .decodeList<ProfileRow>()
            .map { UserSearchResult(it.id, it.username, it.displayName, it.avatarUrl) }
    }.getOrDefault(emptyList())

    suspend fun fetchProfile(userId: String): OtherUserProfile? = runCatching {
        val uid = me()
        val r = db.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileRow>()
        val isFollowing = runCatching {
            db.from("follows")
                .select { filter { eq("follower_id", uid); eq("following_id", userId) } }
                .decodeList<FollowRow>()
                .isNotEmpty()
        }.getOrDefault(false)
        OtherUserProfile(
            r.id, r.username, r.displayName, r.avatarUrl,
            r.bannerUrl, r.bio, r.followerCount, r.followingCount,
            r.isVerified, isFollowing
        )
    }.getOrNull()

    suspend fun follow(userId: String) {
        db.from("follows").upsert(FollowRow(me(), userId))
    }

    suspend fun unfollow(userId: String) {
        db.from("follows").delete {
            filter {
                eq("follower_id", me())
                eq("following_id", userId)
            }
        }
    }
}