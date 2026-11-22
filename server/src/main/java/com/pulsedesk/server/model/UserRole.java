package com.pulsedesk.server.model;

/**
 * Các vai trò người dùng trong hệ thống PulseDesk.
 */
public enum UserRole {
    CUSTOMER("Khách hàng"),
    STAFF("Nhân viên tư vấn CSKH"),
    ADMIN("Quản trị viên hệ thống");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
