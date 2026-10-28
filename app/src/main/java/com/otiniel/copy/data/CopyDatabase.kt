package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: Long
)

data class MemoryEntry(
    val id: Long,
    val title: String,
    val content: String,
    val category: String,
    val createdAt: Long
)

/** Local-first store. This class never sends messages to a network. */
class CopyDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT 'manual',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_created_at ON messages(created_at, id)")
        db.execSQL("CREATE INDEX idx_memories_created_at ON memories(created_at, id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit, non-destructive migrations when the schema changes.
    }

    @Synchronized
    fun ensureWelcomeMessage() {
        val count = readableDatabase.rawQuery("SELECT COUNT(*) FROM messages", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (count == 0) {
            insertMessage(
                role = "assistant",
                content = "Sou o COPY. O histórico e as memórias ficam no aparelho. Configure um provedor em Configurações para ativar respostas de IA; nenhuma conexão externa é usada antes disso."
            )
        }
    }

    fun getMessages(limit: Int = 250): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        readableDatabase.rawQuery(
            "SELECT id, role, content, created_at FROM messages ORDER BY created_at ASC, id ASC LIMIT ?",
            arrayOf(limit.coerceIn(1, 1000).toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += ChatMessage(
                    id = cursor.getLong(0),
                    role = cursor.getString(1),
                    content = cursor.getString(2),
                    createdAt = cursor.getLong(3)
                )
            }
        }
        return result
    }

    fun insertMessage(role: String, content: String): ChatMessage {
        require(role == "user" || role == "assistant") { "Invalid message role." }
        val cleanContent = content.trim().take(MAX_MESSAGE_LENGTH)
        require(cleanContent.isNotEmpty()) { "Message cannot be empty." }
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("role", role)
            put("content", cleanContent)
            put("created_at", now)
        }
        val id = writableDatabase.insertOrThrow("messages", null, values)
        return ChatMessage(id, role, cleanContent, now)
    }

    fun getMemories(search: String = ""): List<MemoryEntry> {
        val result = mutableListOf<MemoryEntry>()
        val query = search.trim()
        val sql = if (query.isBlank()) {
            "SELECT id, title, content, category, created_at FROM memories ORDER BY created_at DESC, id DESC"
        } else {
            "SELECT id, title, content, category, created_at FROM memories WHERE title LIKE ? OR content LIKE ? ORDER BY created_at DESC, id DESC"
        }
        val args = if (query.isBlank()) null else arrayOf("%" + query + "%", "%" + query + "%")
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result += MemoryEntry(
                    id = cursor.getLong(0),
                    title = cursor.getString(1),
                    content = cursor.getString(2),
                    category = cursor.getString(3),
                    createdAt = cursor.getLong(4)
                )
            }
        }
        return result
    }

    fun insertMemory(title: String, content: String, category: String = "manual"): MemoryEntry {
        val cleanContent = content.trim().take(MAX_MEMORY_LENGTH)
        require(cleanContent.isNotEmpty()) { "Memory cannot be empty." }
        val cleanTitle = title.trim().ifBlank { cleanContent.take(48) }.take(80)
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", cleanTitle)
            put("content", cleanContent)
            put("category", category.take(32))
            put("created_at", now)
        }
        val id = writableDatabase.insertOrThrow("memories", null, values)
        return MemoryEntry(id, cleanTitle, cleanContent, category, now)
    }

    companion object {
        private const val DATABASE_NAME = "copy_local.db"
        private const val DATABASE_VERSION = 1
        private const val MAX_MESSAGE_LENGTH = 20_000
        private const val MAX_MEMORY_LENGTH = 10_000
    }
}
