package com.pulsedesk.app.network

import android.util.Log
import com.pulsedesk.app.model.SocketMessagePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

enum class SocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

sealed class SocketEvent {
    data class Connected(val host: String, val port: Int) : SocketEvent()
    data class Disconnected(val reason: String?) : SocketEvent()
    data class PacketReceived(val packet: SocketMessagePacket) : SocketEvent()
    data class RawMessageReceived(val raw: String) : SocketEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : SocketEvent()
}

/**
 * SocketClientManager: TCP Socket chạy ngầm kết nối tới Backend cổng 8888.
 * Toàn bộ tác vụ mạng chạy trên Dispatchers.IO.
 */
class SocketClientManager(
    private val host: String = "10.0.2.2",
    private val port: Int = 8888,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "PulseDesk_Socket"
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var connectionJob: Job? = null
    private var shouldAutoReconnect: Boolean = true

    fun start() {
        shouldAutoReconnect = true
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch(Dispatchers.IO) {
            connectLoop()
        }
    }

    fun stop() {
        shouldAutoReconnect = false
        connectionJob?.cancel()
        closeSocket()
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    private suspend fun connectLoop() = withContext(Dispatchers.IO) {
        while (isActive && shouldAutoReconnect) {
            try {
                _connectionState.value = SocketConnectionState.CONNECTING
                val currentSocket = Socket()
                currentSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket = currentSocket

                writer = BufferedWriter(OutputStreamWriter(currentSocket.getOutputStream(), Charsets.UTF_8))
                reader = BufferedReader(InputStreamReader(currentSocket.getInputStream(), Charsets.UTF_8))

                _connectionState.value = SocketConnectionState.CONNECTED
                _events.emit(SocketEvent.Connected(host, port))

                readIncomingStream(reader!!)
            } catch (e: Exception) {
                if (isActive) {
                    _events.emit(SocketEvent.Error(e.message ?: "Connection error", e))
                    _connectionState.value = SocketConnectionState.RECONNECTING
                }
            } finally {
                closeSocket()
            }

            if (isActive && shouldAutoReconnect) {
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun readIncomingStream(reader: BufferedReader) = withContext(Dispatchers.IO) {
        try {
            while (isActive) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    _events.emit(SocketEvent.RawMessageReceived(line))
                    parseAndEmitPacket(line)
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                _events.emit(SocketEvent.Disconnected(e.message))
            }
        }
    }

    private suspend fun parseAndEmitPacket(raw: String) {
        try {
            val json = JSONObject(raw)
            val packet = SocketMessagePacket(
                type = json.optString("type", "NEW_MESSAGE"),
                conversationId = json.optString("conversationId", ""),
                message = json.optString("message", null),
                unreadCount = if (json.has("unreadCount")) json.getInt("unreadCount") else null,
                timestamp = json.optString("timestamp", null),
                isOnline = if (json.has("isOnline")) json.getBoolean("isOnline") else null
            )
            _events.emit(SocketEvent.PacketReceived(packet))
        } catch (_: Exception) {
            // Bỏ qua nếu dữ liệu không phải định dạng JSON
        }
    }

    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentWriter = writer
            if (currentWriter != null && _connectionState.value == SocketConnectionState.CONNECTED) {
                currentWriter.write(message)
                currentWriter.newLine()
                currentWriter.flush()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun closeSocket() {
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        writer = null
        reader = null
        socket = null
    }
}
