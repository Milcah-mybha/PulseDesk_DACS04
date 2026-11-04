package com.pulsedesk.app.data.local

import kotlinx.coroutines.flow.Flow

interface ConversationDao {
    fun getAll(): Flow<List<ConversationEntity>>
    suspend fun getById(id: String): ConversationEntity?
    suspend fun insertAll(conversations: List<ConversationEntity>)
    suspend fun insert(conversation: ConversationEntity)
    suspend fun markAsRead(id: String)
    suspend fun updateMessage(
        id: String,
        message: String,
        unreadCount: Int,
        timestamp: String,
        updatedAt: Long
    )
    suspend fun count(): Int
}
