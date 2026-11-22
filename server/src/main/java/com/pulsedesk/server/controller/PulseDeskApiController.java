package com.pulsedesk.server.controller;

import com.pulsedesk.server.model.*;
import com.pulsedesk.server.repository.ConversationRepository;
import com.pulsedesk.server.repository.KnowledgeBaseRepository;
import com.pulsedesk.server.repository.MessageRepository;
import com.pulsedesk.server.service.DatabaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST API Controller phục vụ tích hợp đa kênh (Omnichannel Web, Mobile, CRM)
 * và kết nối trực tiếp với cơ sở dữ liệu MongoDB Atlas.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PulseDeskApiController {

    private final DatabaseService databaseService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public PulseDeskApiController(
            DatabaseService databaseService,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            KnowledgeBaseRepository knowledgeBaseRepository) {
        this.databaseService = databaseService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    /**
     * 1. Health check cơ sở dữ liệu MongoDB Atlas
     */
    @GetMapping("/health/db")
    public CompletableFuture<ResponseEntity<DatabaseService.PingResult>> checkDbHealth() {
        return databaseService.pingAsync()
                .thenApply(result -> result.success()
                        ? ResponseEntity.ok(result)
                        : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result));
    }

    /**
     * 2. API Đăng nhập xác thực với MongoDB Atlas (hỗ trợ Staff & Admin)
     */
    @PostMapping("/auth/login")
    public CompletableFuture<ResponseEntity<?>> login(@RequestBody Map<String, String> request) {
        String identifier = request.getOrDefault("identifier", "");
        String password = request.getOrDefault("password", "");

        if (identifier.isBlank() || password.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Thiếu email/staffId hoặc password"))
            );
        }

        return databaseService.authenticateAsync(identifier, password)
                .thenApply(userOpt -> {
                    if (userOpt.isPresent()) {
                        UserDocument user = userOpt.get();
                        return ResponseEntity.ok(Map.of(
                                "status", "SUCCESS",
                                "id", user.getId(),
                                "email", user.getEmail(),
                                "staffId", user.getStaffId() != null ? user.getStaffId() : "",
                                "fullName", user.getFullName(),
                                "role", user.getRole().name(),
                                "roleTitle", user.getRole().getDescription()
                        ));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                "status", "FAILED",
                                "error", "Email hoặc mật khẩu không chính xác"
                        ));
                    }
                });
    }

    /**
     * 3. API Đăng ký người dùng mới và lưu trực tiếp vào MongoDB Atlas
     */
    @PostMapping("/auth/register")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> register(@RequestBody Map<String, String> request) {
        String fullName = request.getOrDefault("fullName", "").trim();
        String email = request.getOrDefault("email", "").trim();
        String staffId = request.getOrDefault("staffId", "").trim();
        String password = request.getOrDefault("password", "");
        String roleStr = request.getOrDefault("role", "STAFF").toUpperCase();

        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "Vui lòng điền đầy đủ Họ tên, Email và Mật khẩu!"))
            );
        }

        UserRole role = UserRole.STAFF;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception ignored) {}

        return databaseService.registerUserAsync(fullName, email, staffId.isEmpty() ? null : staffId, password, role)
                .<ResponseEntity<Map<String, Object>>>handle((user, ex) -> {
                    if (ex != null) {
                        String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        return ResponseEntity.badRequest().body(Map.of(
                                "status", "FAILED",
                                "error", errMsg != null ? errMsg : "Lỗi đăng ký"
                        ));
                    }
                    return ResponseEntity.ok(Map.of(
                            "status", "SUCCESS",
                            "message", "Đăng ký thành công và đã lưu vào MongoDB Atlas!",
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "staffId", user.getStaffId() != null ? user.getStaffId() : "",
                            "fullName", user.getFullName(),
                            "role", user.getRole().name(),
                            "roleTitle", user.getRole().getDescription()
                    ));
                });
    }

    /**
     * 3. Lấy danh sách cuộc hội thoại / ticket CSKH
     */
    @GetMapping("/conversations")
    public CompletableFuture<ResponseEntity<List<ConversationDocument>>> getConversations(
            @RequestParam(required = false) String staffId) {
        if (staffId != null && !staffId.isBlank()) {
            return databaseService.getStaffConversationsAsync(staffId)
                    .thenApply(ResponseEntity::ok);
        }
        return CompletableFuture.supplyAsync(
                () -> ResponseEntity.ok(conversationRepository.findAll())
        );
    }

    /**
     * 4. Lấy lịch sử tin nhắn của một cuộc hội thoại từ MongoDB Atlas
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public CompletableFuture<ResponseEntity<List<MessageDocument>>> getMessages(
            @PathVariable String conversationId) {
        return databaseService.getConversationMessagesAsync(conversationId)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * 5. Gửi tin nhắn mới và lưu trực tiếp vào MongoDB Atlas
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public CompletableFuture<ResponseEntity<MessageDocument>> sendMessage(
            @PathVariable String conversationId,
            @RequestBody Map<String, Object> body) {

        String senderId = (String) body.getOrDefault("senderId", "staff_01");
        String senderName = (String) body.getOrDefault("senderName", "Nhân viên CSKH");
        String senderTypeStr = (String) body.getOrDefault("senderType", "STAFF");
        String content = (String) body.getOrDefault("content", "");

        SenderType senderType = SenderType.STAFF;
        try {
            senderType = SenderType.valueOf(senderTypeStr.toUpperCase());
        } catch (Exception ignored) {}

        FileAttachmentInfo attachment = null;
        if (body.containsKey("attachment") && body.get("attachment") instanceof Map<?, ?> attMap) {
            Object rawSize = attMap.get("fileSizeBytes");
            long size = (rawSize instanceof Number) ? ((Number) rawSize).longValue() : 0L;
            attachment = new FileAttachmentInfo(
                    (String) attMap.get("fileName"),
                    (String) attMap.get("fileType"),
                    size,
                    (String) attMap.get("fileUrl"),
                    (String) attMap.get("sha256")
            );
        }

        return databaseService.saveIncomingMessageAsync(
                conversationId, senderId, senderName, senderType, content, attachment
        ).thenApply(ResponseEntity::ok);
    }

    /**
     * 6. Tìm kiếm tri thức AI RAG (Retrieval-Augmented Generation)
     */
    @GetMapping("/knowledge-base/search")
    public CompletableFuture<ResponseEntity<List<KnowledgeBaseDocument>>> searchKnowledge(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "5") int limit) {
        return databaseService.searchKnowledgeBaseAsync(q, limit)
                .thenApply(ResponseEntity::ok);
    }
}
