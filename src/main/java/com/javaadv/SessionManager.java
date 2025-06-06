package com.javaadv;

public class SessionManager {
    private static SessionManager instance;
    private String accessToken;
    private String refreshToken;

    private SessionManager() {
        // Private constructor để ngăn khởi tạo trực tiếp
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setRefreshToken(String token) {
        this.refreshToken = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
    }

    // Phương thức bổ sung để cập nhật cả accessToken và refreshToken (nếu cần)
    public void updateTokens(String newAccessToken, String newRefreshToken) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
    }
}