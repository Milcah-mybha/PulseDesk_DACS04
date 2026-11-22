package com.pulsedesk.server;

import com.pulsedesk.server.model.UserDocument;
import com.pulsedesk.server.model.UserRole;
import com.pulsedesk.server.service.DatabaseService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MongoAtlasRegisterTest {

    private static final Logger log = LoggerFactory.getLogger(MongoAtlasRegisterTest.class);

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void testRegisterUserDirectlyToMongoDBAtlas() {
        String testEmail = "test_user_" + System.currentTimeMillis() + "@pulsedesk.vn";
        String testStaffId = "staff_test_" + (System.currentTimeMillis() % 10000);
        String rawPassword = "password123";

        log.info("Thực hiện đăng ký user mới lên MongoDB Atlas: {}", testEmail);

        // 1. Gọi đăng ký
        UserDocument created = databaseService.registerUserAsync(
                "Bùi Hà My",
                testEmail,
                testStaffId,
                rawPassword,
                UserRole.ADMIN
        ).join();

        assertNotNull(created.getId(), "User mới phải có ID sinh bởi MongoDB");
        log.info("✅ Đã tạo user mới thành công trên MongoDB Atlas với ID: {}", created.getId());

        // 2. Kiểm tra trực tiếp từ MongoDB Template
        Query query = new Query(Criteria.where("email").is(testEmail));
        UserDocument found = mongoTemplate.findOne(query, UserDocument.class);

        assertNotNull(found, "User phải tìm thấy trong collection 'users' trên MongoDB Atlas");
        assertEquals("Bùi Hà My", found.getFullName());
        assertEquals(UserRole.ADMIN, found.getRole());
        assertNotEquals(rawPassword, found.getPasswordHash(), "Mật khẩu phải được băm BCrypt!");

        // 3. Kiểm tra xác thực (Authenticate)
        Optional<UserDocument> auth = databaseService.authenticateAsync(testEmail, rawPassword).join();
        assertTrue(auth.isPresent(), "Phải đăng nhập thành công với mật khẩu vừa tạo!");

        log.info("🎉 XÁC THỰC THÀNH CÔNG USER MỚI TRÊN MONGODB ATLAS:");
        log.info(" - Email: {}", found.getEmail());
        log.info(" - Role: {}", found.getRole());
        log.info(" - BCrypt Hash: {}", found.getPasswordHash());

        // Cleanup test user
        mongoTemplate.remove(query, UserDocument.class);
        log.info("Đã dọn dẹp test user sau khi kiểm tra xong.");
    }
}
