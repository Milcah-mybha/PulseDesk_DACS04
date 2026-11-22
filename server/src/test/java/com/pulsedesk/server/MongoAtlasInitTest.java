package com.pulsedesk.server;

import com.pulsedesk.server.service.DatabaseService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MongoAtlasInitTest {

    private static final Logger log = LoggerFactory.getLogger(MongoAtlasInitTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DatabaseService databaseService;

    @Test
    void testConnectAndVerifyDatabaseOnAtlas() {
        log.info("Bắt đầu kiểm tra kết nối tới MongoDB Atlas...");

        // 1. Ping Atlas
        DatabaseService.PingResult ping = databaseService.pingAsync().join();
        log.info("Kết quả Ping Atlas: success={}, latency={} ms, db={}",
                ping.success(), ping.latencyMs(), ping.databaseName());
        assertTrue(ping.success(), "Kết nối tới MongoDB Atlas phải thành công!");

        // 2. Kiểm tra các collection đã được tạo
        Set<String> collections = mongoTemplate.getCollectionNames();
        log.info("Danh sách Collections đã tạo trên MongoDB Atlas: {}", collections);

        assertTrue(collections.contains("users"), "Phải có collection 'users'");
        assertTrue(collections.contains("conversations"), "Phải có collection 'conversations'");
        assertTrue(collections.contains("messages"), "Phải có collection 'messages'");
        assertTrue(collections.contains("knowledge_base"), "Phải có collection 'knowledge_base'");

        // 3. Đếm số lượng document
        long userCount = mongoTemplate.getCollection("users").countDocuments();
        long convCount = mongoTemplate.getCollection("conversations").countDocuments();
        long msgCount = mongoTemplate.getCollection("messages").countDocuments();
        long kbCount = mongoTemplate.getCollection("knowledge_base").countDocuments();

        log.info("=================================================");
        log.info("✅ DỮ LIỆU ĐÃ TỒN TẠI TRÊN MONGODB ATLAS:");
        log.info(" - users: {} documents", userCount);
        log.info(" - conversations: {} documents", convCount);
        log.info(" - messages: {} documents", msgCount);
        log.info(" - knowledge_base: {} documents", kbCount);
        log.info("=================================================");

        assertTrue(userCount >= 2, "Phải có ít nhất 2 users");
        assertTrue(convCount >= 1, "Phải có ít nhất 1 conversation");
        assertTrue(msgCount >= 5, "Phải có ít nhất 5 messages");
        assertTrue(kbCount >= 2, "Phải có ít nhất 2 knowledge base articles");
    }
}
