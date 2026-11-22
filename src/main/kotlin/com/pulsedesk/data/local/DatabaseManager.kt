package com.pulsedesk.data.local

import com.pulsedesk.data.MockData
import com.pulsedesk.model.ChatMessage
import com.pulsedesk.model.Conversation
import com.pulsedesk.model.ConversationCategory
import com.pulsedesk.model.MessageSenderType
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    private const val DB_FILE = "pulsedesk.db"
    private const val DB_URL = "jdbc:sqlite:$DB_FILE"

    init {
        initDatabase()
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection(DB_URL)
    }

    private fun initDatabase() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT PRIMARY KEY,
                        customer_name TEXT NOT NULL,
                        topic TEXT NOT NULL,
                        is_vip INTEGER NOT NULL,
                        is_online INTEGER NOT NULL,
                        last_message TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        unread_count INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        avatar_initials TEXT NOT NULL,
                        avatar_color_index INTEGER NOT NULL
                    );
                    """.trimIndent()
                )

                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT PRIMARY KEY,
                        conversation_id TEXT NOT NULL,
                        sender_type TEXT NOT NULL,
                        message TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        is_delivered INTEGER NOT NULL DEFAULT 1
                    );
                    """.trimIndent()
                )

                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    );
                    """.trimIndent()
                )
            }
        }
        seedIfEmpty()
    }

    private fun seedIfEmpty() {
        getConnection().use { conn ->
            val count = conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT COUNT(*) FROM conversations")
                if (rs.next()) rs.getInt(1) else 0
            }
            if (count == 0) {
                // Nạp dữ liệu mẫu ban đầu vào SQLite
                MockData.conversations.forEach { conv ->
                    insertConversationInternal(conn, conv)
                    val msgs = MockData.getMessagesFor(conv.id)
                    msgs.forEach { msg ->
                        insertMessageInternal(conn, msg)
                    }
                }
            }
        }
    }

    private fun insertConversationInternal(conn: Connection, conv: Conversation) {
        val sql = """
            INSERT OR REPLACE INTO conversations 
            (id, customer_name, topic, is_vip, is_online, last_message, timestamp, unread_count, category, avatar_initials, avatar_color_index)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, conv.id)
            pstmt.setString(2, conv.customerName)
            pstmt.setString(3, conv.topic)
            pstmt.setInt(4, if (conv.isVip) 1 else 0)
            pstmt.setInt(5, if (conv.isOnline) 1 else 0)
            pstmt.setString(6, conv.lastMessage)
            pstmt.setString(7, conv.timestamp)
            pstmt.setInt(8, conv.unreadCount)
            pstmt.setString(9, conv.category.name)
            pstmt.setString(10, conv.avatarInitials)
            pstmt.setInt(11, conv.avatarColorIndex)
            pstmt.executeUpdate()
        }
    }

    private fun insertMessageInternal(conn: Connection, msg: ChatMessage) {
        val sql = """
            INSERT OR REPLACE INTO messages 
            (id, conversation_id, sender_type, message, timestamp, is_delivered)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, msg.id)
            pstmt.setString(2, msg.conversationId)
            pstmt.setString(3, msg.senderType.name)
            pstmt.setString(4, msg.message)
            pstmt.setString(5, msg.timestamp)
            pstmt.setInt(6, if (msg.isDelivered) 1 else 0)
            pstmt.executeUpdate()
        }
    }

    fun getAllConversations(): List<Conversation> {
        val list = mutableListOf<Conversation>()
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM conversations")
                while (rs.next()) {
                    val catStr = rs.getString("category")
                    val cat = try {
                        ConversationCategory.valueOf(catStr)
                    } catch (_: Exception) {
                        ConversationCategory.ALL
                    }
                    list.add(
                        Conversation(
                            id = rs.getString("id"),
                            customerName = rs.getString("customer_name"),
                            topic = rs.getString("topic"),
                            isVip = rs.getInt("is_vip") == 1,
                            isOnline = rs.getInt("is_online") == 1,
                            lastMessage = rs.getString("last_message"),
                            timestamp = rs.getString("timestamp"),
                            unreadCount = rs.getInt("unread_count"),
                            category = cat,
                            avatarInitials = rs.getString("avatar_initials"),
                            avatarColorIndex = rs.getInt("avatar_color_index")
                        )
                    )
                }
            }
        }
        return list
    }

    fun getMessagesForConversation(convId: String): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY rowid ASC"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, convId)
                val rs = pstmt.executeQuery()
                while (rs.next()) {
                    val senderStr = rs.getString("sender_type")
                    val sender = try {
                        MessageSenderType.valueOf(senderStr)
                    } catch (_: Exception) {
                        MessageSenderType.CUSTOMER
                    }
                    list.add(
                        ChatMessage(
                            id = rs.getString("id"),
                            conversationId = rs.getString("conversation_id"),
                            senderType = sender,
                            message = rs.getString("message"),
                            timestamp = rs.getString("timestamp"),
                            isDelivered = rs.getInt("is_delivered") == 1
                        )
                    )
                }
            }
        }
        return list
    }

    fun saveMessage(msg: ChatMessage) {
        getConnection().use { conn ->
            insertMessageInternal(conn, msg)
        }
    }

    fun updateConversation(conv: Conversation) {
        getConnection().use { conn ->
            insertConversationInternal(conn, conv)
        }
    }

    fun markAsRead(convId: String) {
        getConnection().use { conn ->
            val sql = "UPDATE conversations SET unread_count = 0 WHERE id = ?"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, convId)
                pstmt.executeUpdate()
            }
        }
    }

    fun getSetting(key: String, defaultValue: String): String {
        getConnection().use { conn ->
            val sql = "SELECT value FROM settings WHERE key = ?"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, key)
                val rs = pstmt.executeQuery()
                if (rs.next()) return rs.getString("value")
            }
        }
        return defaultValue
    }

    fun saveSetting(key: String, value: String) {
        getConnection().use { conn ->
            val sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)"
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, key)
                pstmt.setString(2, value)
                pstmt.executeUpdate()
            }
        }
    }
}
