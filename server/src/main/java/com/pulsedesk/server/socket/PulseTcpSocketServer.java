package com.pulsedesk.server.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsedesk.server.model.FileAttachmentInfo;
import com.pulsedesk.server.model.KnowledgeBaseDocument;
import com.pulsedesk.server.model.MessageDocument;
import com.pulsedesk.server.model.SenderType;
import com.pulsedesk.server.service.DatabaseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * TCP Socket Server thời gian thực cho hệ thống PulseDesk (mặc định cổng 8888).
 * Tích hợp trực tiếp với MongoDB Atlas:
 * - Tự động lưu mọi tin nhắn gửi nhận vào Cloud.
 * - Broadcast tin nhắn hai chiều giữa Khách hàng và Nhân viên CSKH.
 * - Tự động kích hoạt AI RAG từ Knowledge Base trên MongoDB khi có câu hỏi mới.
 */
@Component
public class PulseTcpSocketServer {

    private static final Logger log = LoggerFactory.getLogger(PulseTcpSocketServer.class);

    @Value("${pulsedesk.tcp-server.port:8888}")
    private int port;

    private final DatabaseService databaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    // Quản lý danh sách các client đang kết nối đồng thời (Thread-safe)
    private final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();

    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "pulsedesk-socket-worker");
        t.setDaemon(true);
        return t;
    });

    private Thread serverAcceptThread;

    public PulseTcpSocketServer(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostConstruct
    public void startServer() {
        serverAcceptThread = new Thread(this::listenForClients, "pulsedesk-tcp-acceptor");
        serverAcceptThread.setDaemon(true);
        serverAcceptThread.start();
    }

    private void listenForClients() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            log.info("⚡ PulseDesk Real-Time TCP Socket Server started on PORT: {}", port);
            log.info("   - Protocol: Newline-Delimited JSON UTF-8");
            log.info("   - Storage Engine: MongoDB Atlas Cloud (Live)");

            while (isRunning && !serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler);
                clientExecutor.submit(handler);
            }
        } catch (Exception ex) {
            if (isRunning) {
                log.error("Lỗi TCP Socket Server: {}", ex.getMessage());
            }
        }
    }

    @PreDestroy
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : activeClients) {
                client.closeConnection();
            }
            activeClients.clear();
            clientExecutor.shutdownNow();
            log.info("PulseDesk TCP Socket Server đã dừng an toàn.");
        } catch (Exception ex) {
            log.warn("Lỗi khi dừng TCP Socket Server: {}", ex.getMessage());
        }
    }

    /**
     * Broadcast tin nhắn tới tất cả client đang kết nối (trừ người gửi nếu cần).
     */
    public void broadcastJson(String jsonLine, ClientHandler excludeClient) {
        for (ClientHandler client : activeClients) {
            if (client != excludeClient) {
                client.sendRaw(jsonLine);
            }
        }
    }

    /**
     * Lớp đại diện cho một phiên kết nối Client (Desktop App hoặc Web Client).
     */
    public class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String clientIp;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                clientIp = socket.getRemoteSocketAddress().toString();
                log.info("🟢 Client kết nối thành công: {}", clientIp);

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

                // Gửi tin nhắn chào mừng (Welcome Packet)
                Map<String, Object> welcome = Map.of(
                        "type", "SYSTEM",
                        "sender", "SYSTEM",
                        "message", "Connected to PulseDesk Server (MongoDB Atlas Live)",
                        "timestamp", System.currentTimeMillis()
                );
                sendRaw(objectMapper.writeValueAsString(welcome));

                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    handleIncomingPacket(line);
                }
            } catch (Exception ex) {
                log.debug("Client {} ngắt kết nối: {}", clientIp, ex.getMessage());
            } finally {
                closeConnection();
                activeClients.remove(this);
                log.info("🔴 Client đã thoát: {} (Còn lại: {} clients)", clientIp, activeClients.size());
            }
        }

        private void handleIncomingPacket(String rawJson) {
            try {
                JsonNode root = objectMapper.readTree(rawJson);
                String convId = root.path("conversationId").asText("conv_1");
                String senderStr = root.path("sender").asText("CUSTOMER");
                String text = root.path("message").asText("");
                String action = root.path("action").asText("MESSAGE");

                SenderType senderType = SenderType.CUSTOMER;
                if ("AGENT".equalsIgnoreCase(senderStr) || "STAFF".equalsIgnoreCase(senderStr)) {
                    senderType = SenderType.STAFF;
                } else if ("ADMIN".equalsIgnoreCase(senderStr)) {
                    senderType = SenderType.ADMIN;
                }

                log.info("📩 Nhận tin nhắn TCP [{}]: conv={}, sender={}, text='{}'",
                        clientIp, convId, senderType, text);

                // 1. Lưu tin nhắn trực tiếp vào MongoDB Atlas (Bất đồng bộ)
                databaseService.saveIncomingMessageAsync(
                        convId,
                        senderType.name().toLowerCase() + "_client",
                        senderType == SenderType.CUSTOMER ? "Customer" : "Staff Member",
                        senderType,
                        text,
                        null
                ).thenAccept(savedMsg -> {
                    // 2. Broadcast tin nhắn tới các client khác
                    try {
                        Map<String, Object> outbound = Map.of(
                                "id", savedMsg.getId(),
                                "conversationId", convId,
                                "sender", senderStr,
                                "message", text,
                                "timestamp", System.currentTimeMillis()
                        );
                        broadcastJson(objectMapper.writeValueAsString(outbound), this);
                    } catch (Exception e) {
                        log.error("Lỗi serialize broadcast: {}", e.getMessage());
                    }
                });

                // 3. Nếu tin nhắn từ Khách hàng -> Tìm kiếm AI RAG từ Knowledge Base trên Atlas
                if (senderType == SenderType.CUSTOMER && !text.isBlank()) {
                    triggerAiRagSuggestion(convId, text);
                }

            } catch (Exception ex) {
                log.warn("Lỗi phân tích JSON từ client: {} - Raw: {}", ex.getMessage(), rawJson);
            }
        }

        private void triggerAiRagSuggestion(String convId, String userQuestion) {
            databaseService.searchKnowledgeBaseAsync(userQuestion, 2)
                    .thenAccept(kbList -> {
                        if (!kbList.isEmpty()) {
                            KnowledgeBaseDocument bestMatch = kbList.get(0);
                            String suggestion = "AI Suggestion: " + bestMatch.getTitle();
                            try {
                                Map<String, Object> aiPacket = Map.of(
                                        "type", "AI_SUGGESTION",
                                        "conversationId", convId,
                                        "sender", "AI_ASSISTANT",
                                        "message", suggestion,
                                        "source", bestMatch.getSource(),
                                        "content", bestMatch.getContent(),
                                        "timestamp", System.currentTimeMillis()
                                );
                                String aiJson = objectMapper.writeValueAsString(aiPacket);
                                // Gửi gợi ý AI tới tất cả nhân viên tư vấn
                                for (ClientHandler c : activeClients) {
                                    c.sendRaw(aiJson);
                                }
                                log.info("💡 Đã gửi gợi ý AI RAG từ MongoDB: '{}'", bestMatch.getTitle());
                            } catch (Exception e) {
                                log.error("Lỗi gửi AI suggestion: {}", e.getMessage());
                            }
                        }
                    });
        }

        public void sendRaw(String text) {
            try {
                if (writer != null) {
                    writer.println(text);
                }
            } catch (Exception ignored) {}
        }

        public void closeConnection() {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }
}
