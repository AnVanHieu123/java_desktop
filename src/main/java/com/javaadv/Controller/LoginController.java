package com.javaadv.Controller;

import com.javaadv.SceneManager;
import com.javaadv.Services.AuthService;
import com.javaadv.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.regex.Pattern;

public class LoginController {
    @FXML private Label lblError;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    private AuthService authService;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    @FXML
    public void initialize() {
        authService = new AuthService();
        txtUsername.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(new ActionEvent());
            }
        });
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin(new ActionEvent());
            }
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        lblError.setText("");
        lblError.setStyle("");

        if (username.isEmpty()) {
            showError("Vui lòng nhập email!", txtUsername);
            return;
        }
        if (!EMAIL_PATTERN.matcher(username).matches()) {
            showError("Email không hợp lệ!", txtUsername);
            return;
        }
        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu!", txtPassword);
            return;
        }

        try {
            String accessToken = authService.login(username, password);
            SessionManager.getInstance().setAccessToken(accessToken); // Lưu token vào SessionManager

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            if (stage == null) {
                showError("Lỗi hệ thống: Không tìm thấy cửa sổ ứng dụng!");
                return;
            }
            SceneManager.changeScene(stage, "/com/fxml/Dashboard.fxml", "Phần mềm quản lý");

        } catch (HttpTimeoutException e) {
            showError("Mất kết nối với server. Vui lòng thử lại!");
        } catch (IOException e) {
            showError("Lỗi kết nối: " + e.getMessage());
        } catch (InterruptedException e) {
            showError("Quá trình đăng nhập bị gián đoạn");
        } catch (Exception e) {
            String errorMessage = parseApiError(e.getMessage());
            showError(errorMessage);
        }
    }

    private void showError(String message, Control... fields) {
        lblError.setText(message);
        lblError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        FadeTransition ft = new FadeTransition(Duration.millis(3000), lblError);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(2000));
        ft.play();

        for (Control field : fields) {
            shakeTextField(field);
        }
    }

    private String parseApiError(String errorMessage) {
        if (errorMessage.contains("Đăng nhập thất bại")) {
            return "Tài khoản hoặc mật khẩu không đúng!";
        }
        return errorMessage;
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