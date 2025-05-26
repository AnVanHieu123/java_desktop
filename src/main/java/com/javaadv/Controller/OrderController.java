package com.javaadv.Controller;

import com.javaadv.Model.Order;
import com.javaadv.SceneManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderController {

    // Thay thế bằng token mới nhất từ Postman
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ4MDg5NjMxLCJleHAiOjE3NDk1Mjk2MzF9.pg0xxGoUuG0WBreYZjsYbHdz3GSnexunzz4sasrllQ0";
    private static final String BASE_URL = "http://localhost:8081/api/admin/orders";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML private Button btnOverview;
    @FXML private Button btnUserManagement;
    @FXML private Button btnCategoryManagement;
    @FXML private Button btnProductManagement;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> colId;
    @FXML private TableColumn<Order, String> colOrderCode;
    @FXML private TableColumn<Order, BigDecimal> colTotalPrice;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, String> colFullName;
    @FXML private TableColumn<Order, String> colAddress;
    @FXML private TableColumn<Order, String> colPaymentStatus;
    @FXML private TableColumn<Order, Timestamp> colCreatedAt;
    @FXML private TableColumn<Order, Timestamp> colUpdatedAt;

    @FXML private TextField txtId;
    @FXML private TextField txtOrderCode;
    @FXML private TextField txtTotalPrice;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField txtFullName;
    @FXML private TextField txtAddress;
    @FXML private TextField txtPaymentStatus;
    @FXML private TextField txtCreatedAt;
    @FXML private TextField txtUpdatedAt;
    @FXML private TextField txtSearch;

    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    private static final List<String> VALID_STATUSES = Arrays.asList("PENDING", "CONFIRMED", "PREPARING", "PROCESSING", "DELIVERING", "DELIVERED", "CANCELLED");

    @FXML
    public void initialize() {
        // Cấu hình các cột trong TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderCode.setCellValueFactory(new PropertyValueFactory<>("orderCode"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        // Đặt các trường chỉ đọc
        txtId.setEditable(false);
        txtOrderCode.setEditable(false);
        txtTotalPrice.setEditable(false);
        txtFullName.setEditable(false);
        txtAddress.setEditable(false);
        txtPaymentStatus.setEditable(false);
        txtCreatedAt.setEditable(false);
        txtUpdatedAt.setEditable(false);

        // Khởi tạo ComboBox
        statusCombo.setItems(FXCollections.observableArrayList(VALID_STATUSES));

        setupSearchFunctionality();
        fetchOrderList();

        // Listener để hiển thị chi tiết khi chọn đơn hàng
        orderTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showOrderDetails(newValue));
    }

    private void fetchOrderList() {
        new Thread(() -> {
            try {
                String apiUrl = BASE_URL + "/list";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonNode root = objectMapper.readTree(response.toString());
                    JsonNode data = root.path("data");
                    JsonNode items = data.path("items");
                    List<Order> orders = objectMapper.convertValue(items, new com.fasterxml.jackson.core.type.TypeReference<List<Order>>(){});
                    Platform.runLater(() -> {
                        orderList.clear();
                        if (items.isMissingNode() || data.isMissingNode()) {
                            showAlert("Lỗi", "Dữ liệu từ API không hợp lệ.");
                        } else if (orders != null) {
                            orderList.addAll(orders);
                            orderTable.setItems(orderList);
                            if (orders.isEmpty()) {
                                showAlert("Thông báo", "Không có đơn hàng nào.");
                            }
                        }
                    });
                } else {
                    Platform.runLater(() -> showAlert("Lỗi", "Không thể lấy danh sách đơn hàng: HTTP " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void setupSearchFunctionality() {
        FilteredList<Order> filteredData = new FilteredList<>(orderList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(order -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(order.getId()).contains(lowerCaseFilter) ||
                        (order.getOrderCode() != null && order.getOrderCode().toLowerCase().contains(lowerCaseFilter)) ||
                        (order.getStatus() != null && order.getStatus().toLowerCase().contains(lowerCaseFilter)) ||
                        (order.getFullName() != null && order.getFullName().toLowerCase().contains(lowerCaseFilter)) ||
                        (order.getAddress() != null && order.getAddress().toLowerCase().contains(lowerCaseFilter)) ||
                        (order.getPaymentStatus() != null && order.getPaymentStatus().toLowerCase().contains(lowerCaseFilter));
            });
        });

        SortedList<Order> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedData);
    }

    private void showOrderDetails(Order order) {
        if (order != null) {
            txtId.setText(String.valueOf(order.getId()));
            txtOrderCode.setText(order.getOrderCode() != null ? order.getOrderCode() : "");
            txtTotalPrice.setText(order.getTotalPrice() != null ? order.getTotalPrice().toString() : "");
            statusCombo.setValue(order.getStatus() != null ? order.getStatus() : null);
            txtFullName.setText(order.getFullName() != null ? order.getFullName() : "");
            txtAddress.setText(order.getAddress() != null ? order.getAddress() : "");
            txtPaymentStatus.setText(order.getPaymentStatus() != null ? order.getPaymentStatus() : "");
            txtCreatedAt.setText(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
            txtUpdatedAt.setText(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : "");
        } else {
            clearFields();
        }
    }

    @FXML
    private void updateOrder() {
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert("Cảnh báo", "Vui lòng chọn một đơn hàng để cập nhật.");
            return;
        }

        String newStatus = statusCombo.getValue();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng chọn trạng thái mới.");
            return;
        }
        if (!VALID_STATUSES.contains(newStatus)) {
            showAlert("Lỗi", "Trạng thái không hợp lệ. Vui lòng chọn một trạng thái từ danh sách.");
            return;
        }

        try {
            // Chuẩn bị dữ liệu gửi lên API (chỉ gửi status)
            Map<String, String> updateData = new HashMap<>();
            updateData.put("status", newStatus.toLowerCase()); // API yêu cầu chữ thường

            // Gửi yêu cầu POST trực tiếp
            String apiUrl = BASE_URL + "/status/" + selectedOrder.getId();
            executeApiCall("POST", apiUrl, updateData, "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
        }
    }

    private void executeApiCall(String method, String urlStr, Object data, String successMessage) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                if (data != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(objectMapper.writeValueAsBytes(data));
                    }
                }

                int code = conn.getResponseCode();
                String responseMessage = "";
                if (code >= 400) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        responseMessage = response.toString();
                    } catch (Exception e) {
                        responseMessage = "Lỗi không xác định: " + e.getMessage();
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        responseMessage = response.toString();
                    }
                }

                conn.disconnect();

                final String finalResponseMessage = responseMessage;
                Platform.runLater(() -> {
                    if (code == 200) {
                        showAlert("Thành công", successMessage);
                        fetchOrderList();
                        clearFields();
                    } else {
                        String errorMsg = "Thao tác thất bại. Mã lỗi: " + code;
                        if (!finalResponseMessage.isEmpty()) {
                            try {
                                JsonNode errorJson = objectMapper.readTree(finalResponseMessage);
                                String detailedMessage = errorJson.path("message").asText("Không có thông tin chi tiết.");
                                errorMsg += "\nChi tiết: " + detailedMessage;
                            } catch (Exception e) {
                                errorMsg += "\nChi tiết: " + finalResponseMessage;
                            }
                        }
                        showAlert("Lỗi", errorMsg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleClear() {
        clearFields();
        orderTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        txtId.clear();
        txtOrderCode.clear();
        txtTotalPrice.clear();
        statusCombo.setValue(null);
        txtFullName.clear();
        txtAddress.clear();
        txtPaymentStatus.clear();
        txtCreatedAt.clear();
        txtUpdatedAt.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleOverview(ActionEvent event) {
        Stage stage = (Stage) btnOverview.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Dashboard.fxml", "Tổng quan");
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/UserManagement.fxml", "Quản lý Người dùng");
    }

    @FXML
    public void handleProductManagement(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý Sản phẩm");
    }

    @FXML
    public void handleCategoryManagement(ActionEvent event) {
        Stage stage = (Stage) btnCategoryManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/CategoryManagement.fxml", "Quản lý Danh mục");
    }

    @FXML
    public void handleLogout(ActionEvent event) throws java.io.IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), stage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fxml/Login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 825, 400);

                stage.setTitle("ADMIN SYSTEM");
                stage.setScene(scene);
                stage.centerOnScreen();

                FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.25), scene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        });
        fadeOut.play();
    }
}