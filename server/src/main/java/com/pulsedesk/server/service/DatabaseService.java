package com.pulsedesk.server.service;

import com.pulsedesk.server.model.*;
import com.pulsedesk.server.repository.ConversationRepository;
import com.pulsedesk.server.repository.KnowledgeBaseRepository;
import com.pulsedesk.server.repository.MessageRepository;
import com.pulsedesk.server.repository.UserRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service trung gian bao bọc các thao tác truy xuất MongoDB một cách bất đồng bộ (Non-blocking),
 * an toàn để tầng TCP Socket Server (Netty/NIO worker pool) gọi mà không làm tắc nghẽn luồng mạng.
 */
@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PasswordEncoder passwordEncoder;

    // Dedicated Thread Pool dành riêng cho các tác vụ I/O cơ sở dữ liệu
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(
            Math.max(8, Runtime.getRuntime().availableProcessors() * 2),
            r -> {
                Thread t = new Thread(r, "pulsedesk-db-worker");
                t.setDaemon(true);
                return t;
            }
    );

    public DatabaseService(
            MongoTemplate mongoTemplate,
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * DTO kết quả kiểm tra ping cơ sở dữ liệu.
     */
    public record PingResult(boolean success, long latencyMs, String databaseName, String message) {}

    /**
     * Kiểm tra ping kết nối MongoDB bất đồng bộ.
     */
    public CompletableFuture<PingResult> pingAsync() {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                Document ping = mongoTemplate.getDb().runCommand(new Document("ping", 1));
                long latency = System.currentTimeMillis() - start;
                return new PingResult(true, latency, mongoTemplate.getDb().getName(), "Pong: " + ping.get("ok"));
            } catch (Exception ex) {
                long latency = System.currentTimeMillis() - start;
                log.error("Lỗi ping MongoDB: {}", ex.getMessage());
                return new PingResult(false, latency, "N/A", ex.getMessage());
            }
        }, dbExecutor);
    }

    /**
     * Xác thực thông tin đăng nhập của nhân viên/admin qua Staff ID hoặc Email.
     * So khớp mật khẩu với BCrypt Hash an toàn.
     */
    public CompletableFuture<Optional<UserDocument>> authenticateAsync(String identifier, String rawPassword) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<UserDocument> userOpt = userRepository.findByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByStaffId(identifier);
            }

            if (userOpt.isPresent()) {
                UserDocument user = userOpt.get();
                if (user.isActive() && passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                    log.info("Xác thực thành công cho user: {} ({})", user.getEmail(), user.getRole());
                    return Optional.of(user);
                }
            }
            log.warn("Xác thực thất bại cho identifier: {}", identifier);
            return Optional.empty();
        }, dbExecutor);
    }

    /**
     * Đăng ký tài khoản người dùng mới và lưu trực tiếp vào MongoDB Atlas.
     */
    public CompletableFuture<UserDocument> registerUserAsync(
            String fullName,
            String email,
            String staffId,
            String rawPassword,
            UserRole role) {
        return CompletableFuture.supplyAsync(() -> {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email '" + email + "' đã tồn tại trong hệ thống!");
            }
            if (staffId != null && !staffId.isBlank() && userRepository.existsByStaffId(staffId)) {
                throw new IllegalArgumentException("Mã nhân viên / ID '" + staffId + "' đã tồn tại!");
            }

            String hashed = passwordEncoder.encode(rawPassword);
            UserDocument newUser = new UserDocument(email, staffId, fullName, hashed, role);
            UserDocument saved = userRepository.save(newUser);
            log.info("🎉 Đã tạo và lưu người dùng mới vào MongoDB Atlas: {} (Role: {})", saved.getEmail(), saved.getRole());
            return saved;
        }, dbExecutor);
    }

    /**
     * Lưu tin nhắn thời gian thực và cập nhật đồng thời trạng thái của Conversation
     * (tin nhắn cuối, thời gian cuối, tăng unreadCount).
     */
    public CompletableFuture<MessageDocument> saveIncomingMessageAsync(
            String conversationId,
            String senderId,
            String senderName,
            SenderType senderType,
            String content,
            FileAttachmentInfo attachment) {

        return CompletableFuture.supplyAsync(() -> {
            // 1. Tạo và lưu message vào collection 'messages'
            MessageDocument message = new MessageDocument(
                    conversationId, senderId, senderName, senderType, content, attachment
            );
            MessageDocument savedMessage = messageRepository.save(message);

            // 2. Trích xuất snippet nội dung cho danh sách bên trái
            String snippet = content;
            if (snippet.length() > 60) {
                snippet = snippet.substring(0, 57) + "...";
            }
            if (attachment != null && snippet.isBlank()) {
                snippet = "[Tệp: " + attachment.getFileName() + "]";
            }

            // 3. Cập nhật Conversation theo phương thức nguyên tử (atomic update)
            Query convQuery = new Query(Criteria.where("_id").is(conversationId));
            Update convUpdate = new Update()
                    .set("lastMessageSnippet", snippet)
                    .set("lastMessageAt", savedMessage.getTimestamp())
                    .set("updatedAt", Instant.now());

            if (senderType == SenderType.CUSTOMER) {
                convUpdate.inc("unreadCount", 1);
            }

            mongoTemplate.updateFirst(convQuery, convUpdate, ConversationDocument.class);

            return savedMessage;
        }, dbExecutor);
    }

    /**
     * Tải lịch sử tin nhắn của một cuộc hội thoại theo thứ tự thời gian tăng dần.
     */
    public CompletableFuture<List<MessageDocument>> getConversationMessagesAsync(String conversationId) {
        return CompletableFuture.supplyAsync(() ->
                messageRepository.findByConversationIdOrderByTimestampAsc(conversationId),
                dbExecutor
        );
    }

    /**
     * Lấy danh sách cuộc hội thoại đang hoạt động của nhân viên.
     */
    public CompletableFuture<List<ConversationDocument>> getStaffConversationsAsync(String staffId) {
        return CompletableFuture.supplyAsync(() ->
                conversationRepository.findActiveTicketsForStaff(staffId),
                dbExecutor
        );
    }

    /**
     * Tìm kiếm kho tri thức phục vụ gợi ý phản hồi AI RAG (Retrieval-Augmented Generation).
     */
    public CompletableFuture<List<KnowledgeBaseDocument>> searchKnowledgeBaseAsync(String queryText, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (queryText == null || queryText.isBlank()) {
                return knowledgeBaseRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, limit));
            }
            try {
                TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(queryText);
                return knowledgeBaseRepository.findAllBy(criteria, PageRequest.of(0, limit));
            } catch (Exception ex) {
                log.warn("Text search fallback: {}", ex.getMessage());
                return knowledgeBaseRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, limit));
            }
        }, dbExecutor);
    }

    /**
     * Tạo dữ liệu mẫu ban đầu (Initial Seed Data) nếu database trống.
     */
    public CompletableFuture<Void> seedInitialDataIfEmptyAsync() {
        return CompletableFuture.runAsync(() -> {
            if (userRepository.count() == 0) {
                log.info("Khởi tạo dữ liệu mẫu PulseDesk vào MongoDB...");

                // 1. Tạo tài khoản mẫu (Staff & Admin)
                UserDocument staff = new UserDocument(
                        "staff_01@pulsedesk.vn",
                        "staff_01",
                        "Trần Văn Anh",
                        passwordEncoder.encode("123456"),
                        UserRole.STAFF
                );
                UserDocument admin = new UserDocument(
                        "admin@pulsedesk.vn",
                        "admin_01",
                        "Lê Thị Bình (Admin)",
                        passwordEncoder.encode("admin123"),
                        UserRole.ADMIN
                );
                userRepository.saveAll(List.of(staff, admin));

                // 2. Tạo cuộc hội thoại mẫu
                ConversationDocument conv1 = new ConversationDocument(
                        "cust_01", "Lê Thị Bình", "binh.le@example.com",
                        staff.getStaffId(), staff.getFullName(), "Product Return Request"
                );
                conv1.setLastMessageSnippet("Here is the PDF of the return receipt...");
                conv1.setLastMessageAt(Instant.now().minusSeconds(180));
                conv1.setUnreadCount(1);
                ConversationDocument savedConv = conversationRepository.save(conv1);

                // 3. Tạo tin nhắn mẫu kèm tệp đính kèm SHA-256
                FileAttachmentInfo pdfReceipt = new FileAttachmentInfo(
                        "return_receipt.pdf",
                        "application/pdf",
                        154200L,
                        "/attachments/return_receipt.pdf",
                        "ash256287283843639299a"
                );

                List<MessageDocument> initialMessages = List.of(
                        new MessageDocument(savedConv.getId(), "cust_01", "Lê Thị Bình", SenderType.CUSTOMER,
                                "Hello, I am having an issue with the product refund. Can you help?", null),
                        new MessageDocument(savedConv.getId(), staff.getStaffId(), "Trần Văn Anh", SenderType.STAFF,
                                "Hi Binh! Yes, I can certainly check that refund for you. Let me view your transaction.", null),
                        new MessageDocument(savedConv.getId(), "cust_01", "Lê Thị Bình", SenderType.CUSTOMER,
                                "", pdfReceipt),
                        new MessageDocument(savedConv.getId(), staff.getStaffId(), "Trần Văn Anh", SenderType.STAFF,
                                "Let me check the document you sent.", null),
                        new MessageDocument(savedConv.getId(), "cust_01", "Lê Thị Bình", SenderType.CUSTOMER,
                                "Here is the PDF of the return receipt...", null)
                );
                messageRepository.saveAll(initialMessages);

                // 4. Tạo kho tri thức Knowledge Base cho AI RAG
                KnowledgeBaseDocument kb1 = new KnowledgeBaseDocument(
                        "Chính sách Đổi trả và Hoàn tiền 30 ngày",
                        "Khách hàng có quyền yêu cầu đổi trả hoặc hoàn tiền trong vòng 30 ngày kể từ ngày nhận hàng với điều kiện sản phẩm còn nguyên tem mác, hộp và hóa đơn mua hàng (return_receipt.pdf). Thời gian xử lý hoàn tiền từ 3-5 ngày làm việc qua tài khoản ngân hàng ban đầu.",
                        "Chính sách CSKH SME v2.4",
                        List.of("return", "refund", "receipt", "policy")
                );
                KnowledgeBaseDocument kb2 = new KnowledgeBaseDocument(
                        "Quy trình xử lý sự cố thanh toán và đối soát hóa đơn",
                        "Khi khách hàng gặp lỗi Billing Issue hoặc bị trừ tiền hai lần, nhân viên CSKH yêu cầu mã giao dịch ngân hàng và biên lai. Tiến hành đối soát với cổng thanh toán trong vòng 2 giờ.",
                        "Hướng dẫn Kế toán nội bộ",
                        List.of("billing", "invoice", "payment", "dispute")
                );
                knowledgeBaseRepository.saveAll(List.of(kb1, kb2));

                log.info("Khởi tạo dữ liệu mẫu hoàn tất (2 users, 1 conversation, 5 messages, 2 KB docs).");
            }
        }, dbExecutor);
    }
}
