package com.pulsedesk.server.config;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình kết nối cơ sở dữ liệu MongoDB và kiểm tra sức khỏe hệ thống (Health Check)
 * khi khởi động ứng dụng PulseDesk Server.
 */
@Configuration
public class MongoConnectionConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionConfig.class);

    @Value("${pulsedesk.mongodb.min-pool-size:10}")
    private int minPoolSize;

    @Value("${pulsedesk.mongodb.max-pool-size:100}")
    private int maxPoolSize;

    @Value("${pulsedesk.mongodb.max-wait-time-ms:2000}")
    private long maxWaitTimeMs;

    @Value("${pulsedesk.mongodb.connect-timeout-ms:3000}")
    private long connectTimeoutMs;

    @Value("${pulsedesk.mongodb.socket-timeout-ms:5000}")
    private long socketTimeoutMs;

    @Value("${pulsedesk.mongodb.server-selection-timeout-ms:3000}")
    private long serverSelectionTimeoutMs;

    /**
     * Tùy chỉnh Connection Pool & Timeout cho MongoDB Driver,
     * đảm bảo luồng TCP Socket Server không bị nghẽn (connection starvation).
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientSettingsCustomizer() {
        return builder -> {
            builder.applyToConnectionPoolSettings(pool ->
                    pool.minSize(minPoolSize)
                        .maxSize(maxPoolSize)
                        .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
            );
            builder.applyToSocketSettings(socket ->
                    socket.connectTimeout((int) connectTimeoutMs, TimeUnit.MILLISECONDS)
                          .readTimeout((int) socketTimeoutMs, TimeUnit.MILLISECONDS)
            );
            builder.applyToClusterSettings(cluster ->
                    cluster.serverSelectionTimeout(serverSelectionTimeoutMs, TimeUnit.MILLISECONDS)
            );
            log.info("MongoDB Connection Pool configured: minSize={}, maxSize={}, maxWaitMs={}",
                    minPoolSize, maxPoolSize, maxWaitTimeMs);
        };
    }

    /**
     * Loại bỏ trường '_class' tự sinh của Spring Data MongoDB để tối ưu dung lượng lưu trữ
     * và tương thích dữ liệu JSON thuần khi tích hợp với các hệ thống AI/RAG bên ngoài.
     */
    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory databaseFactory,
            MongoMappingContext mappingContext) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }

    /**
     * PasswordEncoder chuẩn mã hóa bảo mật BCrypt với salt factor 12 (chống brute-force).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Startup Check: Ping trực tiếp đến MongoDB khi server vừa bật.
     * Cung cấp thông báo lỗi chi tiết, thân thiện nếu MongoDB chưa được bật hoặc sai mật khẩu.
     */
    @Bean
    public ApplicationRunner mongoStartupCheck(MongoTemplate mongoTemplate, MongoClient mongoClient) {
        return args -> {
            log.info("=================================================================");
            log.info("🚀 PulseDesk MongoDB Connection Startup Verification...");
            long startTime = System.currentTimeMillis();

            try {
                // Gửi lệnh ping tới database
                Document pingResult = mongoTemplate.getDb().runCommand(new Document("ping", 1));
                long latency = System.currentTimeMillis() - startTime;

                String dbName = mongoTemplate.getDb().getName();
                log.info("✅ KẾT NỐI MONGODB THÀNH CÔNG!");
                log.info("   - Database: {}", dbName);
                log.info("   - Độ trễ phản hồi (Latency): {} ms", latency);
                log.info("   - Ping Status: {}", pingResult.get("ok"));
                log.info("   - Ready for Real-Time TCP Socket & AI RAG operations.");
                log.info("=================================================================");
            } catch (Exception ex) {
                log.error("❌ KẾT NỐI MONGODB THẤT BÀI!");
                log.error("   - Lỗi chi tiết: {}", ex.getMessage());
                log.warn("⚠️ HƯỚNG DẪN KHẮC PHỤC:");
                log.warn("   1. Kiểm tra MongoDB Service đã khởi động chưa: `sudo systemctl status mongod` hoặc `docker ps`");
                log.warn("   2. Kiểm tra chuỗi kết nối trong 'application.yml' hoặc biến môi trường 'MONGODB_URI'");
                log.warn("   3. Khởi động nhanh MongoDB qua Docker nếu cần:");
                log.warn("      docker run -d -p 27017:27017 --name pulsedesk-mongo mongo:latest");
                log.info("=================================================================");
            }
        };
    }
}
