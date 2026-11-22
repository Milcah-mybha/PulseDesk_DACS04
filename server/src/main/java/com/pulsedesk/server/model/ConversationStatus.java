package com.pulsedesk.server.model;

/**
 * Trạng thái của một phiên hội thoại / ticket tư vấn CSKH.
 */
public enum ConversationStatus {
    ACTIVE("Đang hoạt động"),
    PENDING("Chờ phản hồi"),
    RESOLVED("Đã giải quyết"),
    CLOSED("Đã đóng");

    private final String description;

    ConversationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
