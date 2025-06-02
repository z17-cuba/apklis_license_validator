package cu.uci.android.apklis_license_validator

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class WebSocketClient(
    private val listener: WebSocketEventListener? = null
) {
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private var pingJob: Job? = null
    private var connectionJob: Job? = null
    private var deviceId: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val subscribedChannels = mutableSetOf<String>()
    private val pendingMessages = mutableListOf<String>()

    companion object {
        private const val PING_INTERVAL = 30_000L // 30 seconds
        private const val CONNECTION_TIMEOUT = 10_000L // 10 seconds
        private const val READ_TIMEOUT = 30_000L // 30 seconds
        private const val TAG = "WebSocketManager"
        private const val WS_URL = "wss://pubsub.mprc.cu"
    }

    fun init(deviceId: String) {
        this.deviceId = deviceId
        initializeHttpClient()
    }

    private fun initializeHttpClient() {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    fun connectAndSubscribe() {
        if (_connectionState.value.isConnected) {
            listener?.onError("Already connected")
            return
        }

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(WS_URL)
                    .build()

                webSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _connectionState.value = ConnectionState(isConnected = true)
                        listener?.onConnected()

                        // Send CONNECT message
                        sendConnectMessage()

                        // Subscribe to the user channel after connection
                        subscribeToChannel(deviceId ?: "")

                        // Start ping-pong mechanism
                        startPingPong()

                        // Send any pending messages
                        sendPendingMessages()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleIncomingMessage(text)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        handleIncomingMessage(bytes.utf8())
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        _connectionState.value = ConnectionState(isConnected = false)
                        listener?.onDisconnected(reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        cleanup()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _connectionState.value = ConnectionState(
                            isConnected = false,
                            error = t.message
                        )
                        listener?.onError(t.message ?: "Connection failed")
                        cleanup()
                    }
                })

            } catch (e: Exception) {
                _connectionState.value = ConnectionState(
                    isConnected = false,
                    error = e.message
                )
                listener?.onError(e.message ?: "Failed to connect")
            }
        }
    }

    private fun sendConnectMessage() {
        // 1. Enviar mensaje CONNECT (requerido por NATS)
        val connectMsg : String = "CONNECT {\"verbose\":false,\"pedantic\":false,\"tls_required\":false}\r\n";
        webSocket?.send(connectMsg);
        Log.d(TAG, "CONNECT enviado");
    }

    private fun handleIncomingMessage(text: String) {
        try {

            if (!isJsonString(text)) {
                // Not JSON, handle as plain text
                Log.d(TAG, "Received plain text message: $text")
                handlePlainTextMessage(text)
            }

        } catch (e: Exception) {
            listener?.onError("Failed to parse message: ${e.message}")
        }
    }


    /**
     * Handle plain text messages (likely NATS protocol)
     */
    private fun handlePlainTextMessage(message: String) {
        // Process NATS protocol messages
        // Common NATS messages: INFO, MSG, +OK, -ERR, PING, PONG

        when {
            message.startsWith("INFO ") -> {
                // Server information
                Log.d(TAG, "NATS server info received")
            }
            message.startsWith("MSG ") -> {
                processNatsMessage(message)
            }
            message.startsWith("CONNECTED ") -> {
                // Connection confirmed, resubscribe to channels if any
                subscribedChannels.forEach { channel ->
                    subscribeToChannel(channel)
                }
            }
            message.startsWith("-ERR ") -> {
                // Error message
                Log.e(TAG, "Server returned error: ${message.substring(5)}")
            }
            message.startsWith("PONG") -> {
                // Response to our PING
                Log.d(TAG, "Received PONG from server")
                _connectionState.value = _connectionState.value.copy(
                    lastPingTime = System.currentTimeMillis()
                )
            }
            else -> {
                // Unknown message format
                Log.w(TAG, "Unrecognized message format: $message")
            }
        }
    }

    /**
     * Process NATS MSG format messages
     */
    private fun processNatsMessage(message: String) {
        // Message from a subscription
        // Format: MSG <subject> <sid> [reply-to] <bytes>\r\n<payload>
         try {
            // Split at the first \r\n to separate header from payload
            val parts = message.split("\r\n", limit = 2)
            if (parts.size < 2) {
                Log.w(TAG, "Invalid MSG format - missing payload")
            }

            val header = parts[0]
            val payload = parts[1]

            // Parse the header
            val headerParts = header.split(" ")
            if (headerParts.size < 4) {
                Log.w(TAG, "Invalid MSG header format: $header")
            }

            val subject = headerParts[1]
            val sid = headerParts[2]

            // Handle message payload - could be JSON or plain text
            Log.d(TAG, "Received message on channel $subject (sid: $sid): $payload")

            // Try to parse payload as JSON if it looks like JSON
            if (isJsonString(payload)) {
                try {
                  /*
                    when (natMessage.type) {
                        "info", "reply", "suggest", "payment"-> {
                            Log.d(TAG, "Notification message received: ${natMessage.type}")

                        }
                       else -> {
                            Log.d(TAG, "Unhandled message type: ${natMessage.type}")
                        }
                    }*/

                    Log.d(TAG, "Message payload is JSON: $payload")
                    // Handle JSON payload - implement specific logic here
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse payload as JSON: $e")
                }
            } else{
                Log.d(TAG, "Message payload is plain text message: $payload")

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MSG command: $e")
        }
    }


    /**
     * Check if a string is valid JSON
     */
    private fun isJsonString(str: String): Boolean {
        return try {
            // Simple check - you might want to use a proper JSON validator
            val trimmed = str.trim()
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))
        } catch (e: Exception) {
            false
        }
    }

   private fun subscribeToChannel(channel: String) {
       // 2. Suscribirse al canal (formato: SUB <canal> <sid>\r\n)
       // "1" es un ID de suscripción arbitrario

       val randomId: String = generateId()
       val subMsg : String = "SUB $channel $randomId\r\n";
        if (_connectionState.value.isConnected) {
            webSocket?.send(subMsg);
            Log.d(TAG, "Suscripción enviada: " + subMsg.trim());
        } else {
            pendingMessages.add(subMsg)
        }
    }

/*    fun unsubscribeFromChannel(channel: String) {
        val unsubMsg = Message(type = "UNSUB", channel = channel)
        sendMessage(unsubMsg)
        subscribedChannels.remove(channel)
    }*/


    private fun sendPendingMessages() {
        pendingMessages.forEach { message ->
            webSocket?.send(message);
        }
        pendingMessages.clear()
    }

    private fun startPingPong() {
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (_connectionState.value.isConnected) {
                delay(PING_INTERVAL)
                if (_connectionState.value.isConnected) {
                    sendPing()
                }
            }
        }
    }

    private fun sendPing() {
        val pingMsg = "PING\r\n".encodeUtf8()
        webSocket?.send(pingMsg);
        Log.d(TAG, "Sent PING message")
    }

    fun disconnect() {
        webSocket?.close(1000, "User requested disconnect")
        cleanup()
    }

    private fun cleanup() {
        pingJob?.cancel()
        connectionJob?.cancel()
        _connectionState.value = ConnectionState(isConnected = false)
        subscribedChannels.clear()
        pendingMessages.clear()
    }

}


data class ConnectionState(
    val isConnected: Boolean = false,
    val error: String? = null,
    val lastPingTime: Long = 0L
)

private fun generateId(): String = Random.nextInt(10000, 99999).toString()

interface WebSocketEventListener {
    fun onConnected()
    fun onDisconnected(reason: String?)
    fun onError(error: String)
}