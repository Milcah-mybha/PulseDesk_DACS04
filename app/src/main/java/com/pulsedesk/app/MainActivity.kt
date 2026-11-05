package com.pulsedesk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pulsedesk.app.ui.screens.ChatDetailScreen
import com.pulsedesk.app.ui.screens.ConversationsListScreen
import com.pulsedesk.app.ui.theme.PulseDeskTheme
import com.pulsedesk.app.viewmodel.ChatDetailViewModel
import com.pulsedesk.app.viewmodel.ConversationsViewModel

class MainActivity : ComponentActivity() {
    private val conversationsViewModel: ConversationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseDeskTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    val selectedConv by conversationsViewModel.selectedConversation.collectAsState()
                    val chatDetailViewModel = remember {
                        ChatDetailViewModel(conversationsViewModel.repository)
                    }

                    BackHandler(enabled = selectedConv != null) {
                        conversationsViewModel.selectConversation(null)
                    }

                    if (selectedConv != null) {
                        ChatDetailScreen(
                            conversation = selectedConv!!,
                            viewModel = chatDetailViewModel,
                            onBackClick = { conversationsViewModel.selectConversation(null) }
                        )
                    } else {
                        ConversationsListScreen(
                            viewModel = conversationsViewModel,
                            onConversationClick = { conversation ->
                                conversationsViewModel.selectConversation(conversation)
                            }
                        )
                    }
                }
            }
        }
    }
}
