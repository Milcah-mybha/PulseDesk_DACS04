package com.pulsedesk.network

import com.pulsedesk.model.ChatMessage
import com.pulsedesk.model.MessageSenderType
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TCP Socket Client real-time kết nối tới TCP Server (port 8888)
class PulseSocketClient(
    private val onMessageReceived: (ChatMessage) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var clientJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isConnected: Boolean = false
        private set

    fun connect(host: String = "127.0.0.1", port: Int = 8888) {
        disconnect()
        clientJob = clientScope.launch {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 3000)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(s.getInputStream()))
                isConnected = true
                withContext(Dispatchers.Main) { onConnectionStatusChanged(true) }

                // Lắng nghe dữ liệu liên tục từ server
                var line: String?
                while (isActive && isConnected) {
                    line = reader?.readLine() ?: break
                    if (line.isNotBlank()) {
                        try {
                            val json = JSONObject(line)
                            val convId = json.optString("conversationId", "conv_1")
                            val senderStr = json.optString("sender", "CUSTOMER")
                            val text = json.optString("message", line)
                            val senderType = when (senderStr.uppercase()) {
                                "AGENT" -> MessageSenderType.AGENT
                                "SYSTEM" -> MessageSenderType.SYSTEM
                                else -> MessageSenderType.CUSTOMER
                            }
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val msg = ChatMessage(
                                id = "sock_${System.currentTimeMillis()}",
                                conversationId = convId,
                                senderType = senderType,
                                message = text,
                                timestamp = time
                            )
                            withContext(Dispatchers.Main) {
                                onMessageReceived(msg)
                            }
                        } catch (e: Exception) {
                            // Nếu không phải JSON, coi như plain text message
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val msg = ChatMessage(
                                id = "sock_${System.currentTimeMillis()}",
                                conversationId = "conv_1",
                                senderType = MessageSenderType.CUSTOMER,
                                message = line,
                                timestamp = time
                            )
                            withContext(Dispatchers.Main) {
                                onMessageReceived(msg)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Kết nối lỗi hoặc ngắt kết nối
            } finally {
                disconnectInternal()
            }
        }
    }

    fun sendMessage(conversationId: String, sender: MessageSenderType, text: String) {
        clientScope.launch {
            try {
                if (isConnected && writer != null) {
                    val json = JSONObject().apply {
                        put("conversationId", conversationId)
                        put("sender", sender.name)
                        put("message", text)
                        put("timestamp", System.currentTimeMillis())
                    }
                    writer?.println(json.toString())
                }
            } catch (e: Exception) {
                // Xử lý khi gửi lỗi
            }
        }
    }

    fun disconnect() {
        clientJob?.cancel()
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}
        isConnected = false
        CoroutineScope(Dispatchers.Main).launch {
            onConnectionStatusChanged(false)
        }
    }
}
