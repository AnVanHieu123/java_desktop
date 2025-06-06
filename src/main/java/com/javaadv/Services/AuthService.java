package com.javaadv.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthService {
    private static final String API_URL = "http://localhost:8081/api/auth/access-token";

    public String login(String username, String password) throws Exception {
        String requestBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", username, password);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse JSON để lấy accessToken
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.body());
            String accessToken = jsonNode.get("accessToken").asText();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("Không tìm thấy accessToken trong phản hồi");
            }

            return accessToken; // Trả về trực tiếp access token
        } else {
            throw new RuntimeException("Đăng nhập thất bại: " + response.body());
        }
    }
}