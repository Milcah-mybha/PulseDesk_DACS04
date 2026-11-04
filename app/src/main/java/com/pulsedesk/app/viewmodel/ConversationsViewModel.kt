package com.pulsedesk.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulsedesk.app.data.local.AppDatabase
import com.pulsedesk.app.data.repository.ConversationRepository
import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.ConversationCategory
import com.pulsedesk.app.network.SocketConnectionState
import com.pulsedesk.app.network.SocketEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val filteredConversations: List<Conversation> = emptyList(),
    val selectedCategory: ConversationCategory = ConversationCategory.ALL,
    val searchQuery: String = "",
    val socketState: SocketConnectionState = SocketConnectionState.DISCONNECTED,
    val lastReceivedEventMessage: String? = null
)

/**
 * ViewModel trong mô hình MVVM:
 * Quản lý trạng thái UI và tương tác với ConversationRepository (Room DB + Socket).
 */
class ConversationsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = ConversationRepository(
        conversationDao = AppDatabase.getInstance(application).conversationDao()
    )

    private val _selectedCategory = MutableStateFlow(ConversationCategory.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _lastEventMessage = MutableStateFlow<String?>(null)

    // Kết hợp luồng dữ liệu từ Room Database với bộ lọc tìm kiếm và danh mục
    val uiState: StateFlow<ConversationsUiState> = combine(
        repository.conversations,
        _selectedCategory,
        _searchQuery,
        repository.socketState,
        _lastEventMessage
    ) { list, category, query, socketState, lastEvent ->
        ConversationsUiState(
            filteredConversations = applyFilter(list, category, query),
            selectedCategory = category,
            searchQuery = query,
            socketState = socketState,
            lastReceivedEventMessage = lastEvent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationsUiState()
    )

    init {
        repository.start()
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            repository.socketEvents.collect { event ->
                when (event) {
                    is SocketEvent.Connected -> _lastEventMessage.value = "Đã kết nối Socket :${event.port}"
                    is SocketEvent.Disconnected -> _lastEventMessage.value = "Mất kết nối Socket"
                    is SocketEvent.PacketReceived -> _lastEventMessage.value = "Tin mới từ #${event.packet.conversationId}"
                    is SocketEvent.Error -> _lastEventMessage.value = "Lỗi Socket: ${event.message}"
                    else -> {}
                }
            }
        }
    }

    fun selectCategory(category: ConversationCategory) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            repository.markAsRead(conversationId)
        }
    }

    fun simulateIncomingSocketMessage() {
        viewModelScope.launch {
            repository.simulateIncomingSocketMessage()
        }
    }

    private fun applyFilter(
        list: List<Conversation>,
        category: ConversationCategory,
        query: String
    ): List<Conversation> {
        return list.filter { conv ->
            val matchesCategory = when (category) {
                ConversationCategory.ALL -> true
                ConversationCategory.UNREAD -> conv.unreadCount > 0
                ConversationCategory.VIP -> conv.isVip
                ConversationCategory.IN_PROGRESS -> conv.category == ConversationCategory.IN_PROGRESS
            }
            val matchesQuery = query.isBlank() ||
                conv.customerName.contains(query, ignoreCase = true) ||
                conv.topic.contains(query, ignoreCase = true) ||
                conv.lastMessage.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stop()
    }
}
