package com.pulsedesk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsedesk.app.data.repository.ConversationRepository
import com.pulsedesk.app.model.ChatMessage
import com.pulsedesk.app.model.Conversation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputMessage: String = "",
    val isSending: Boolean = false,
    val quickReplies: List<String> = emptyList()
)

class ChatDetailViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()
    private var messageCollectionJob: Job? = null

    fun loadConversation(conversation: Conversation) {
        if (_uiState.value.conversation?.id == conversation.id && _uiState.value.messages.isNotEmpty()) {
            return
        }

        messageCollectionJob?.cancel()

        val quickSuggestions = when (conversation.id) {
            "conv_1" -> listOf(
                "Em đang kiểm tra log hệ thống rồi anh nhé!",
                "Anh gửi lại mã hóa đơn để em đối soát gấp.",
                "Sự cố 502 đã được xử lý xong, anh thử lại giúp em."
            )
            "conv_2" -> listOf(
                "Đã nhận chuyển khoản, em nâng cấp gói ngay cho chị!",
                "Hóa đơn VAT đã gửi vào email chị rồi nhé.",
                "Cảm ơn chị đã đồng hành cùng PulseDesk!"
            )
            else -> listOf(
                "Dạ PulseDesk sẵn sàng hỗ trợ bạn ngay bây giờ.",
                "Bạn vui lòng cung cấp thêm thông tin chi tiết ạ.",
                "Yêu cầu của bạn đã được chuyển tới chuyên viên tư vấn."
            )
        }

        _uiState.value = ChatDetailUiState(
            conversation = conversation,
            quickReplies = quickSuggestions
        )

        messageCollectionJob = viewModelScope.launch {
            repository.markAsRead(conversation.id)
            repository.getMessages(conversation.id).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun onInputMessageChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputMessage = text)
    }

    fun sendMessage(text: String? = null) {
        val msgToSend = text ?: _uiState.value.inputMessage
        if (msgToSend.isBlank()) return
        val conv = _uiState.value.conversation ?: return

        _uiState.value = _uiState.value.copy(inputMessage = "")

        viewModelScope.launch {
            repository.sendMessage(conv.id, msgToSend)
        }
    }
}
