package com.javaadv;

import com.javaadv.Model.User;
import com.javaadv.Services.AuthService;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.net.http.HttpTimeoutException;


public class LoginController {
    @FXML private Label lblError;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;



    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // Reset error state
        lblError.setText("");
        lblError.setStyle("");

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        try {
            // 1. Gọi API đăng nhập
            AuthService authService = new AuthService();
            String accessToken = authService.login(username, password);

            // 2. Lưu thông tin user (không lưu password)
//            User user = User.getCurrentUser();
//            user.setUsername(username);
//            user.setAccessToken(accessToken);

            // 3. Chuyển sang Dashboard (giữ nguyên hiệu ứng transition nếu có)
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            SceneManager.changeScene(stage, "/com/fxml/Dashboard.fxml", "Quản lý sinh viên");

        } catch (HttpTimeoutException e) {
            showError("Mất kết nối với server. Vui lòng thử lại!");
        } catch (IOException e) {
            showError("Lỗi kết nối: " + e.getMessage());
        } catch (InterruptedException e) {
            showError("Quá trình đăng nhập bị gián đoạn");
        } catch (Exception e) {
            showError(e.getMessage()); // Hiển thị thông báo lỗi từ API
        }
    }

    // Tách hàm hiển thị lỗi để tái sử dụng
    private void showError(String message) {
        lblError.setText(message);
        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        shakeTextField(txtUsername);
        shakeTextField(txtPassword);
    }





    @FXML
    public void initialize() {
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(new ActionEvent());
            }
        });
    }

    private void shakeTextField(Control field) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(70), field);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }
}
