package com.pulsedesk.app.data.local

import kotlinx.coroutines.flow.Flow

interface ChatMessageDao {
    fun getMessagesByConversation(conversationId: String): Flow<List<ChatMessageEntity>>
    suspend fun insert(message: ChatMessageEntity)
    suspend fun insertAll(messages: List<ChatMessageEntity>)
    suspend fun countByConversation(conversationId: String): Int
}
