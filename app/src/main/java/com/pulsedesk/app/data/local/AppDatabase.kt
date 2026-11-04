package com.pulsedesk.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Cơ sở dữ liệu SQLite cục bộ (chuẩn kiến trúc Room/DAO).
 * Chạy trực tiếp trên Android Runtime, độc lập 100% với các plugin annotation processor (KAPT/KSP).
 */
class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "pulsedesk_local.db",
    null,
    1
) {
    private val dao = SQLiteConversationDao(this)

    fun conversationDao(): ConversationDao = dao

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS conversations (
                id TEXT PRIMARY KEY NOT NULL,
                customerName TEXT NOT NULL,
                avatarInitials TEXT NOT NULL,
                avatarColorIndex INTEGER NOT NULL,
                topic TEXT NOT NULL,
                lastMessage TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                unreadCount INTEGER NOT NULL,
                isOnline INTEGER NOT NULL,
                isVip INTEGER NOT NULL,
                category TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS conversations")
        onCreate(db)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}

private class SQLiteConversationDao(private val dbHelper: AppDatabase) : ConversationDao {
    private val _flow = MutableStateFlow<List<ConversationEntity>>(emptyList())

    init {
        refreshFlowSync()
    }

    private fun refreshFlowSync() {
        _flow.value = queryAllSync()
    }

    private fun queryAllSync(): List<ConversationEntity> {
        val list = mutableListOf<ConversationEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM conversations ORDER BY updatedAt DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToEntity(c))
            }
        }
        return list
    }

    private fun cursorToEntity(c: Cursor): ConversationEntity {
        return ConversationEntity(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            customerName = c.getString(c.getColumnIndexOrThrow("customerName")),
            avatarInitials = c.getString(c.getColumnIndexOrThrow("avatarInitials")),
            avatarColorIndex = c.getInt(c.getColumnIndexOrThrow("avatarColorIndex")),
            topic = c.getString(c.getColumnIndexOrThrow("topic")),
            lastMessage = c.getString(c.getColumnIndexOrThrow("lastMessage")),
            timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
            unreadCount = c.getInt(c.getColumnIndexOrThrow("unreadCount")),
            isOnline = c.getInt(c.getColumnIndexOrThrow("isOnline")) == 1,
            isVip = c.getInt(c.getColumnIndexOrThrow("isVip")) == 1,
            category = c.getString(c.getColumnIndexOrThrow("category")),
            updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"))
        )
    }

    private fun entityToValues(entity: ConversationEntity): ContentValues {
        return ContentValues().apply {
            put("id", entity.id)
            put("customerName", entity.customerName)
            put("avatarInitials", entity.avatarInitials)
            put("avatarColorIndex", entity.avatarColorIndex)
            put("topic", entity.topic)
            put("lastMessage", entity.lastMessage)
            put("timestamp", entity.timestamp)
            put("unreadCount", entity.unreadCount)
            put("isOnline", if (entity.isOnline) 1 else 0)
            put("isVip", if (entity.isVip) 1 else 0)
            put("category", entity.category)
            put("updatedAt", entity.updatedAt)
        }
    }

    override fun getAll(): Flow<List<ConversationEntity>> = _flow.asStateFlow()

    override suspend fun getById(id: String): ConversationEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM conversations WHERE id = ? LIMIT 1", arrayOf(id))
        cursor.use { c ->
            if (c.moveToFirst()) cursorToEntity(c) else null
        }
    }

    override suspend fun insertAll(conversations: List<ConversationEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (item in conversations) {
                db.insertWithOnConflict("conversations", null, entityToValues(item), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshFlowSync()
    }

    override suspend fun insert(conversation: ConversationEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict("conversations", null, entityToValues(conversation), SQLiteDatabase.CONFLICT_REPLACE)
        refreshFlowSync()
    }

    override suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("unreadCount", 0) }
        db.update("conversations", values, "id = ?", arrayOf(id))
        refreshFlowSync()
    }

    override suspend fun updateMessage(
        id: String,
        message: String,
        unreadCount: Int,
        timestamp: String,
        updatedAt: Long
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("lastMessage", message)
            put("unreadCount", unreadCount)
            put("timestamp", timestamp)
            put("updatedAt", updatedAt)
        }
        db.update("conversations", values, "id = ?", arrayOf(id))
        refreshFlowSync()
    }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM conversations", null)
        cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
