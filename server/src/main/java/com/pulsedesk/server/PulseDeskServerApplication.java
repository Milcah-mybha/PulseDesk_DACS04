package com.pulsedesk.server;

import com.pulsedesk.server.service.DatabaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class PulseDeskServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseDeskServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(DatabaseService databaseService) {
        return args -> {
            // Khởi tạo dữ liệu mẫu bất đồng bộ nếu DB trống
            databaseService.seedInitialDataIfEmptyAsync();
        };
    }
}
