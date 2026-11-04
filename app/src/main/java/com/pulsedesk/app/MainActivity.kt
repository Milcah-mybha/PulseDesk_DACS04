package com.pulsedesk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pulsedesk.app.ui.screens.ConversationsListScreen
import com.pulsedesk.app.ui.theme.PulseDeskTheme
import com.pulsedesk.app.viewmodel.ConversationsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ConversationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseDeskTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    ConversationsListScreen(viewModel = viewModel)
                }
            }
        }
    }
}
