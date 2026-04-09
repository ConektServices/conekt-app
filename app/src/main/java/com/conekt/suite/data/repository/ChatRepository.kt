package com.conekt.suite.data.repository

import android.content.Context
import android.net.Uri
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.feature.chat.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable private data class ConvRow(val id: String = "", val type: String = "direct", val name: String? = null, @SerialName("avatar_url") val avatarUrl: String? = null, @SerialName("last_message") val lastMessage: String? = null, @SerialName("last_message_at") val lastMessageAt: String? = null, @SerialName("created_by") val createdBy: String? = null)
@Serializable private data class ConvInsertRow(val type: String, @SerialName("created_by") val createdBy: String)
@Serializable private data class ConvUpdateRow(@SerialName("last_message") val lastMessage: String, @SerialName("last_message_at") val lastMessageAt: String)
@Serializable private data class MemberConvIdRow(@SerialName("conversation_id") val conversationId: String)
@Serializable private data class MemberRow(@SerialName("conversation_id") val conversationId: String, @SerialName("user_id") val userId: String, @SerialName("last_read_at") val lastReadAt: String? = null, @SerialName("is_blocked") val isBlocked: Boolean = false)
@Serializable private data class MemberInsertRow(@SerialName("conversation_id") val conversationId: String, @SerialName("user_id") val userId: String)

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
    @SerialName("created_at")      val createdAt: String = "",
    // These are nullable with defaults — safe even if columns don't exist yet
    @SerialName("reply_to_id")     val replyToId: String? = null,
    @SerialName("reply_preview")   val replyPreview: String? = null,
    val reactions: String? = null
)

@Serializable private data class MsgInsertRow(@SerialName("conversation_id") val conversationId: String, @SerialName("sender_id") val senderId: String, val body: String? = null, @SerialName("message_type") val messageType: String = "text", @SerialName("file_url") val fileUrl: String? = null, @SerialName("file_name") val fileName: String? = null, @SerialName("reply_to_id") val replyToId: String? = null, @SerialName("reply_preview") val replyPreview: String? = null)
@Serializable private data class ProfileRow(val id: String, val username: String, @SerialName("display_name") val displayName: String? = null, @SerialName("avatar_url") val avatarUrl: String? = null, @SerialName("banner_url") val bannerUrl: String? = null, val bio: String? = null, @SerialName("is_verified") val isVerified: Boolean = false, @SerialName("follower_count") val followerCount: Int = 0, @SerialName("following_count") val followingCount: Int = 0)
@Serializable private data class FollowRow(@SerialName("follower_id") val followerId: String, @SerialName("following_id") val followingId: String)
@Serializable private data class StoryAuthorRow(val id: String, val username: String, @SerialName("display_name") val displayName: String? = null, @SerialName("avatar_url") val avatarUrl: String? = null)
@Serializable private data class StoryRow(val id: String, @SerialName("author_id") val authorId: String, @SerialName("media_url") val mediaUrl: String, @SerialName("media_type") val mediaType: String = "image", val caption: String? = null, @SerialName("expires_at") val expiresAt: String = "", val author: StoryAuthorRow)
@Serializable private data class UnreadParams(@SerialName("conv_id") val convId: String, @SerialName("for_user_id") val forUserId: String)
@Serializable private data class MarkReadParams(@SerialName("conv_id") val convId: String)

private object MsgCrypto {
    private const val ALG = "AES/CBC/PKCS5Padding"
    private fun key(a: String, b: String): ByteArray {
        val seed = listOf(a, b).sorted().joinToString("|")
        return java.security.MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
    }
    fun encrypt(plain: String, myUid: String, otherUid: String): String = runCatching {
        val key = SecretKeySpec(key(myUid, otherUid), "AES")
        val cipher = Cipher.getInstance(ALG).also { it.init(Cipher.ENCRYPT_MODE, key) }
        Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }.getOrDefault(plain)
    fun decrypt(ct: String, myUid: String, otherUid: String): String = runCatching {
        val raw = Base64.getDecoder().decode(ct)
        val c = Cipher.getInstance(ALG).also { it.init(Cipher.DECRYPT_MODE, SecretKeySpec(key(myUid, otherUid), "AES"), IvParameterSpec(raw.copyOfRange(0, 16))) }
        String(c.doFinal(raw.copyOfRange(16, raw.size)), Charsets.UTF_8)
    }.getOrDefault(ct)
}

class ChatRepository(private val auth: AuthRepository = AuthRepository()) {
    private val db  = SupabaseProvider.client
    private fun me()  = auth.currentUserId() ?: error("Not signed in")
    private fun now() = java.time.Instant.now().toString()

    suspend fun fetchConversations(): List<ConversationItem> {
        val uid = me()
        val myConvIds = runCatching {
            db.from("conversation_members").select(Columns.raw("conversation_id")) { filter { eq("user_id", uid) } }.decodeList<MemberConvIdRow>().map { it.conversationId }
        }.getOrDefault(emptyList())
        if (myConvIds.isEmpty()) return emptyList()

        return myConvIds.mapNotNull { convId ->
            runCatching {
                val conv = db.from("conversations").select { filter { eq("id", convId) } }.decodeList<ConvRow>().firstOrNull() ?: return@runCatching null
                val allMembers = db.from("conversation_members").select(Columns.raw("conversation_id, user_id, last_read_at, is_blocked")) { filter { eq("conversation_id", convId) } }.decodeList<MemberRow>()
                val myMembership = allMembers.firstOrNull { it.userId == uid }
                val otherMember  = allMembers.firstOrNull { it.userId != uid }
                if (myMembership?.isBlocked == true) return@runCatching null
                val otherProfile = otherMember?.userId?.let { oid -> runCatching { db.from("profiles").select { filter { eq("id", oid) } }.decodeList<ProfileRow>().firstOrNull() }.getOrNull() }
                val unread = runCatching { db.postgrest.rpc("get_unread_count", UnreadParams(convId, uid)).decodeAs<Long>().toInt() }.getOrDefault(0)
                ConversationItem(id = conv.id, name = otherProfile?.displayName ?: otherProfile?.username ?: conv.name ?: "Chat", avatarUrl = otherProfile?.avatarUrl ?: conv.avatarUrl, lastMessage = conv.lastMessage, lastMessageAt = conv.lastMessageAt, unreadCount = unread, otherUserId = otherMember?.userId ?: "", isBlocked = myMembership?.isBlocked ?: false)
            }.getOrNull()
        }.sortedByDescending { it.lastMessageAt }
    }

    suspend fun getOrCreateDm(otherUserId: String): String {
        val uid = me()
        val mine   = runCatching { db.from("conversation_members").select(Columns.raw("conversation_id")) { filter { eq("user_id", uid) } }.decodeList<MemberConvIdRow>().map { it.conversationId }.toSet() }.getOrDefault(emptySet())
        val theirs = runCatching { db.from("conversation_members").select(Columns.raw("conversation_id")) { filter { eq("user_id", otherUserId) } }.decodeList<MemberConvIdRow>().map { it.conversationId }.toSet() }.getOrDefault(emptySet())
        val shared = mine.intersect(theirs)
        if (shared.isNotEmpty()) return shared.first()
        db.from("conversations").insert(ConvInsertRow("direct", uid))
        val convId = db.from("conversations").select { filter { eq("created_by", uid) }; order("created_at", Order.DESCENDING); limit(1) }.decodeList<ConvRow>().firstOrNull()?.id ?: error("Could not fetch new conversation")
        db.from("conversation_members").insert(listOf(MemberInsertRow(convId, uid), MemberInsertRow(convId, otherUserId)))
        return convId
    }

    suspend fun markAsRead(convId: String) { runCatching { db.postgrest.rpc("mark_conversation_read", MarkReadParams(convId)) } }

    suspend fun deleteConversationForMe(convId: String) {
        db.from("conversation_members").delete { filter { eq("conversation_id", convId); eq("user_id", me()) } }
    }

    suspend fun blockUser(convId: String, blocked: Boolean) {
        db.from("conversation_members").update({ set("is_blocked", blocked) }) { filter { eq("conversation_id", convId); eq("user_id", me()) } }
    }

    suspend fun fetchMessages(convId: String, otherUserId: String): List<MessageItem> {
        val uid = me()
        android.util.Log.d("ChatMsg", "fetchMessages: convId=$convId uid=$uid otherUserId=$otherUserId")

        if (convId.isBlank()) {
            android.util.Log.e("ChatMsg", "convId is blank — aborting fetchMessages")
            return emptyList()
        }

        val rows = try {
            db.from("messages")
                .select {
                    filter {
                        eq("conversation_id", convId)
                        // Use neq instead of eq(false) to handle null values safely
                        neq("is_deleted", true)
                    }
                    order("created_at", Order.ASCENDING)
                    limit(200)
                }
                .decodeList<MsgRow>()
        } catch (e: Exception) {
            android.util.Log.e("ChatMsg", "Failed to fetch messages: ${e::class.simpleName}: ${e.message}")
            return emptyList()
        }

        android.util.Log.d("ChatMsg", "Fetched ${rows.size} message rows")

        return rows.mapNotNull { r ->
            try {
                val type = when (r.messageType) {
                    "image" -> MsgType.IMAGE
                    "file"  -> MsgType.FILE
                    "audio" -> MsgType.AUDIO
                    "music" -> MsgType.MUSIC
                    "emoji" -> MsgType.EMOJI
                    else    -> MsgType.TEXT
                }

                val decryptedBody = when (type) {
                    MsgType.TEXT, MsgType.EMOJI -> {
                        if (r.body == null) {
                            null
                        } else if (otherUserId.isBlank()) {
                            // Can't decrypt without otherUserId — show placeholder
                            android.util.Log.w("ChatMsg", "otherUserId is blank, cannot decrypt msg ${r.id}")
                            "[message]"
                        } else {
                            val decrypted = MsgCrypto.decrypt(r.body, uid, otherUserId)
                            android.util.Log.d("ChatMsg", "Decrypted msg ${r.id}: '${decrypted.take(20)}...'")
                            decrypted
                        }
                    }
                    MsgType.MUSIC -> r.body
                    else -> null
                }

                // Safely parse music payload
                var mTitle: String? = null; var mArtist: String? = null
                var mCover: String? = null;  var mFile:  String? = null
                if (type == MsgType.MUSIC && r.body != null) {
                    runCatching {
                        val j = kotlinx.serialization.json.Json
                            .parseToJsonElement(r.body)
                            .let { it as? kotlinx.serialization.json.JsonObject }
                        fun str(k: String) = j?.get(k)
                            ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                            ?.content
                        mTitle = str("title"); mArtist = str("artist")
                        mCover = str("coverUrl")?.ifBlank { null }; mFile = str("fileUrl")
                    }
                }

                // Safely parse reactions (maybe null or malformed in older rows)
                val reactions: Map<String, Int> = runCatching {
                    val raw = r.reactions

                    if (raw.isNullOrBlank() || raw == "{}" || raw == "null") {
                        emptyMap()
                    } else {
                        val j = Json.parseToJsonElement(raw)
                            .jsonObject

                        j.entries.associate { entry: Map.Entry<String, JsonElement> ->
                            val key: String = entry.key
                            val value: Int = (entry.value as? JsonPrimitive)
                                ?.contentOrNull
                                ?.toIntOrNull() ?: 0

                            key to value
                        }
                    }
                }.getOrDefault(emptyMap())

                // Safely parse reply preview
                val reply = if (!r.replyToId.isNullOrBlank() && !r.replyPreview.isNullOrBlank()) {
                    runCatching {
                        val j = kotlinx.serialization.json.Json
                            .parseToJsonElement(r.replyPreview)
                            .let { it as? kotlinx.serialization.json.JsonObject }
                        ReplyPreview(
                            messageId  = r.replyToId,
                            senderName = j?.get("name")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "",
                            preview    = j?.get("text")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "",
                            isMe       = j?.get("isMe")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } == "true"
                        )
                    }.getOrNull()
                } else null

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
                    musicFile   = mFile,
                    replyTo     = reply,
                    reactions   = reactions
                ).also {
                    android.util.Log.d("ChatMsg", "Mapped message ${r.id}: type=${it.type} isMe=${it.isMe} body='${it.body?.take(20)}'")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatMsg", "Failed to map message ${r.id}: ${e.message}")
                null  // skip broken messages instead of crashing
            }
        }
    }

    suspend fun sendText(convId: String, text: String, otherUserId: String, replyTo: ReplyPreview? = null) {
        val uid = me()
        val isEmoji = text.trim().let { t -> t.length <= 2 && t.all { c -> !c.isLetterOrDigit() && !c.isWhitespace() } }
        val (msgType, body) = if (isEmoji) "emoji" to text else "text" to MsgCrypto.encrypt(text, uid, otherUserId)
        val replyJson = replyTo?.let { """{"name":"${it.senderName.replace("\"","'")}","text":"${it.preview.replace("\"","'")}","isMe":"${it.isMe}"}""" }
        db.from("messages").insert(MsgInsertRow(convId, uid, body, msgType, replyToId = replyTo?.messageId, replyPreview = replyJson))
        db.from("conversations").update(ConvUpdateRow(text.take(80), now())) { filter { eq("id", convId) } }
    }

    suspend fun sendFile(context: Context, convId: String, uri: Uri, otherUserId: String) {
        val uid  = me()
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":")?.ifBlank { null } ?: "file_${System.currentTimeMillis()}"
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: error("Could not read file")
        val ext = when { mime.startsWith("image") -> ".jpg"; mime.startsWith("audio") -> ".mp3"; else -> "" }
        val path = "$uid/chat/${System.currentTimeMillis()}$ext"
        db.storage.from("conekt-files").upload(path, bytes) { upsert = true }
        val url = db.storage.from("conekt-files").publicUrl(path)
        val msgType = when { mime.startsWith("image") -> "image"; mime.startsWith("audio") -> "audio"; else -> "file" }
        val preview = when (msgType) { "image" -> "📷 Photo"; "audio" -> "🎵 Audio"; else -> "📎 $name" }
        db.from("messages").insert(MsgInsertRow(convId, uid, messageType = msgType, fileUrl = url, fileName = name))
        db.from("conversations").update(ConvUpdateRow(preview, now())) { filter { eq("id", convId) } }
    }

    suspend fun sendMusic(convId: String, trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        val uid = me()
        val body = """{"trackId":"$trackId","title":"${title.replace("\"","'")}","artist":"${artist.replace("\"","'")}","coverUrl":"${coverUrl.orEmpty()}","fileUrl":"$fileUrl"}"""
        db.from("messages").insert(MsgInsertRow(convId, uid, body = body, messageType = "music"))
        db.from("conversations").update(ConvUpdateRow("🎵 $title", now())) { filter { eq("id", convId) } }
    }

    suspend fun deleteMessageForEveryone(msgId: String) { db.from("messages").update({ set("is_deleted", true) }) { filter { eq("id", msgId) } } }
    suspend fun deleteMessageForMe(msgId: String) { /* ViewModel removes from local state */ }

    suspend fun addReaction(msgId: String, emoji: String, currentReactions: Map<String, Int>) {
        val updated = currentReactions.toMutableMap().also { it[emoji] = (it[emoji] ?: 0) + 1 }
        val json = updated.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }
        db.from("messages").update({ set("reactions", json) }) { filter { eq("id", msgId) } }
    }

    suspend fun fetchStories(): List<StoryThumb> {
        val cols = Columns.raw("id, author_id, media_url, media_type, caption, expires_at, author:profiles!stories_author_id_fkey(id, username, display_name, avatar_url)")
        return runCatching { db.from("stories").select(cols) { filter { gt("expires_at", now()) }; order("created_at", Order.DESCENDING); limit(30) }.decodeList<StoryRow>().map { r -> StoryThumb(r.id, r.authorId, r.author.displayName ?: r.author.username, r.author.avatarUrl, r.mediaUrl, r.caption) } }.getOrDefault(emptyList())
    }

    suspend fun searchUsers(q: String): List<UserSearchResult> = runCatching { db.from("profiles").select { filter { or { ilike("username", "%$q%"); ilike("display_name", "%$q%") } }; limit(20) }.decodeList<ProfileRow>().map { UserSearchResult(it.id, it.username, it.displayName, it.avatarUrl) } }.getOrDefault(emptyList())

    suspend fun fetchProfile(userId: String): OtherUserProfile? = runCatching {
        val uid = me(); val r = db.from("profiles").select { filter { eq("id", userId) } }.decodeSingle<ProfileRow>()
        val isFollowing = runCatching { db.from("follows").select { filter { eq("follower_id", uid); eq("following_id", userId) } }.decodeList<FollowRow>().isNotEmpty() }.getOrDefault(false)
        OtherUserProfile(r.id, r.username, r.displayName, r.avatarUrl, r.bannerUrl, r.bio, r.followerCount, r.followingCount, r.isVerified, isFollowing)
    }.getOrNull()

    suspend fun follow(userId: String)   { db.from("follows").upsert(FollowRow(me(), userId)) }
    suspend fun unfollow(userId: String) { db.from("follows").delete { filter { eq("follower_id", me()); eq("following_id", userId) } } }
}