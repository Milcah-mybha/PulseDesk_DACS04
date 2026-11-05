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

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "pulsedesk_local.db",
    null,
    3
) {
    private val conversationDaoImpl = SQLiteConversationDao(this)
    private val chatMessageDaoImpl = SQLiteChatMessageDao(this)

    fun conversationDao(): ConversationDao = conversationDaoImpl
    fun chatMessageDao(): ChatMessageDao = chatMessageDaoImpl

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createTables(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        try {
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

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id TEXT PRIMARY KEY NOT NULL,
                    conversationId TEXT NOT NULL,
                    senderType TEXT NOT NULL,
                    message TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        } catch (_: Exception) {}
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
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM conversations ORDER BY updatedAt DESC", null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntity(c))
                }
            }
        } catch (_: Exception) {}
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
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM conversations WHERE id = ? LIMIT 1", arrayOf(id))
            cursor.use { c ->
                if (c.moveToFirst()) cursorToEntity(c) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun insertAll(conversations: List<ConversationEntity>) = withContext(Dispatchers.IO) {
        try {
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
        } catch (_: Exception) {}
    }

    override suspend fun insert(conversation: ConversationEntity) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.insertWithOnConflict("conversations", null, entityToValues(conversation), SQLiteDatabase.CONFLICT_REPLACE)
            refreshFlowSync()
        } catch (_: Exception) {}
    }

    override suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply { put("unreadCount", 0) }
            db.update("conversations", values, "id = ?", arrayOf(id))
            refreshFlowSync()
        } catch (_: Exception) {}
    }

    override suspend fun updateMessage(
        id: String,
        message: String,
        unreadCount: Int,
        timestamp: String,
        updatedAt: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("lastMessage", message)
                put("unreadCount", unreadCount)
                put("timestamp", timestamp)
                put("updatedAt", updatedAt)
            }
            db.update("conversations", values, "id = ?", arrayOf(id))
            refreshFlowSync()
        } catch (_: Exception) {}
    }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM conversations", null)
            cursor.use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }
}

private class SQLiteChatMessageDao(private val dbHelper: AppDatabase) : ChatMessageDao {
    private val flowsByConv = mutableMapOf<String, MutableStateFlow<List<ChatMessageEntity>>>()

    private fun getFlowFor(convId: String): MutableStateFlow<List<ChatMessageEntity>> {
        return synchronized(flowsByConv) {
            flowsByConv.getOrPut(convId) {
                MutableStateFlow(queryMessages(convId))
            }
        }
    }

    private fun queryMessages(convId: String): List<ChatMessageEntity> {
        val list = mutableListOf<ChatMessageEntity>()
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM chat_messages WHERE conversationId = ? ORDER BY createdAt ASC",
                arrayOf(convId)
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        ChatMessageEntity(
                            id = c.getString(c.getColumnIndexOrThrow("id")),
                            conversationId = c.getString(c.getColumnIndexOrThrow("conversationId")),
                            senderType = c.getString(c.getColumnIndexOrThrow("senderType")),
                            message = c.getString(c.getColumnIndexOrThrow("message")),
                            timestamp = c.getString(c.getColumnIndexOrThrow("timestamp")),
                            status = c.getString(c.getColumnIndexOrThrow("status")),
                            createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    override fun getMessagesByConversation(conversationId: String): Flow<List<ChatMessageEntity>> {
        return getFlowFor(conversationId).asStateFlow()
    }

    override suspend fun insert(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", message.id)
                put("conversationId", message.conversationId)
                put("senderType", message.senderType)
                put("message", message.message)
                put("timestamp", message.timestamp)
                put("status", message.status)
                put("createdAt", message.createdAt)
            }
            db.insertWithOnConflict("chat_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            getFlowFor(message.conversationId).value = queryMessages(message.conversationId)
        } catch (_: Exception) {}
    }

    override suspend fun insertAll(messages: List<ChatMessageEntity>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (msg in messages) {
                    val values = ContentValues().apply {
                        put("id", msg.id)
                        put("conversationId", msg.conversationId)
                        put("senderType", msg.senderType)
                        put("message", msg.message)
                        put("timestamp", msg.timestamp)
                        put("status", msg.status)
                        put("createdAt", msg.createdAt)
                    }
                    db.insertWithOnConflict("chat_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            val convId = messages.first().conversationId
            getFlowFor(convId).value = queryMessages(convId)
        } catch (_: Exception) {}
    }

    override suspend fun countByConversation(conversationId: String): Int = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM chat_messages WHERE conversationId = ?",
                arrayOf(conversationId)
            )
            cursor.use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }
}
