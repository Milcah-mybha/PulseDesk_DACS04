package com.pulsedesk.server.model;

/**
 * Loại người gửi tin nhắn trong phòng chat thời gian thực.
 */
public enum SenderType {
    CUSTOMER("Khách hàng"),
    STAFF("Nhân viên tư vấn"),
    ADMIN("Quản trị viên"),
    AI_ASSISTANT("Trợ lý ảo AI RAG"),
    SYSTEM("Hệ thống");

    private final String description;

    SenderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
