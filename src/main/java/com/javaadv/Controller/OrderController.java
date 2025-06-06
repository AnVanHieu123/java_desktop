package com.javaadv.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.javaadv.Model.Order;
import com.javaadv.SceneManager;
import com.javaadv.SessionManager;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderController {

    private static final String BASE_URL = "http://localhost:8081/api/admin/orders";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private static final List<String> VALID_STATUSES = Arrays.asList("PENDING", "CONFIRMED", "PREPARING", "PROCESSING", "DELIVERING", "DELIVERED", "CANCELLED");

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

    public OrderController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderCode.setCellValueFactory(new PropertyValueFactory<>("orderCode"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        txtId.setEditable(false);
        txtOrderCode.setEditable(false);
        txtTotalPrice.setEditable(false);
        txtFullName.setEditable(false);
        txtAddress.setEditable(false);
        txtPaymentStatus.setEditable(false);
        txtCreatedAt.setEditable(false);
        txtUpdatedAt.setEditable(false);

        statusCombo.setItems(FXCollections.observableArrayList(VALID_STATUSES));

        setupSearchFunctionality();
        fetchOrderList();

        orderTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showOrderDetails(newValue));
    }

    private void fetchOrderList() {
        new Thread(() -> {
            try {
                String token = SessionManager.getInstance().getAccessToken();
                if (token == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy token. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/list"))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode data = root.path("data");
                    JsonNode items = data.path("items");
                    List<Order> orders = objectMapper.convertValue(items, new com.fasterxml.jackson.core.type.TypeReference<List<Order>>(){});
                    Platform.runLater(() -> {
                        orderList.clear();
                        if (items.isMissingNode() || data.isMissingNode()) {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu từ API không hợp lệ.");
                        } else if (orders != null) {
                            orderList.addAll(orders);
                            orderTable.setItems(orderList);
                            if (orders.isEmpty()) {
                                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không có đơn hàng nào.");
                            }
                        }
                    });
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    Map<String, Object> errorMap = objectMapper.readValue(response.body(), Map.class);
                    String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu: " + message + " (Mã lỗi: " + response.statusCode() + ")"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
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
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một đơn hàng để cập nhật.");
            return;
        }

        String newStatus = statusCombo.getValue();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn trạng thái mới.");
            return;
        }
        if (!VALID_STATUSES.contains(newStatus)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Trạng thái không hợp lệ. Vui lòng chọn một trạng thái từ danh sách.");
            return;
        }

        try {
            Map<String, String> updateData = new HashMap<>();
            updateData.put("status", newStatus.toLowerCase());

            String apiUrl = BASE_URL + "/status/" + selectedOrder.getId();
            executeApiCall("POST", apiUrl, updateData, "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
        }
    }

    private void executeApiCall(String method, String urlStr, Object data, String successMessage) {
        new Thread(() -> {
            try {
                String token = SessionManager.getInstance().getAccessToken();
                if (token == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy token. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                    return;
                }

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(java.time.Duration.ofSeconds(5));

                if (data != null) {
                    String jsonBody = objectMapper.writeValueAsString(data);
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
                        fetchOrderList();
                        clearFields();
                    });
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    Map<String, Object> errorMap = objectMapper.readValue(response.body(), Map.class);
                    String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại: " + message + " (Mã lỗi: " + response.statusCode() + ")"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    private void redirectToLogin() {
        Stage stage = (Stage) orderTable.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Login.fxml", "Đăng nhập");
        SessionManager.getInstance().clearSession();
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        String bgColor = switch (type) {
            case ERROR -> "#f8d7da";
            case WARNING -> "#fff3cd";
            default -> "#d4edda";
        };
        alert.getDialogPane().setStyle("-fx-background-color: " + bgColor + "; -fx-border-radius: 5;");
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
        Stage stage = (Stage) btnProductManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý Sản phẩm");
    }

    @FXML
    public void handleCategoryManagement(ActionEvent event) {
        Stage stage = (Stage) btnCategoryManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/CategoryManagement.fxml", "Quản lý Danh mục");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        if (new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn đăng xuất?", ButtonType.YES, ButtonType.NO)
                .showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
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
                    SessionManager.getInstance().clearSession();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            fadeOut.play();
        }
    }
}