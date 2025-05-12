package com.javaadv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.Model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class DashboardController {

    @FXML private BorderPane mainContent;
    @FXML private Label lblUserName;
    @FXML private ImageView imgAvatar;

    // Summary Labels
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalOrders;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalCustomers;

    // Sales Chart
    @FXML private LineChart<String, Number> salesChart;

    // Top Categories Chart
    @FXML private PieChart topCategoriesChart;

    // Navigation Buttons
    @FXML private Button btnUserManagement;
    @FXML private Button btnProductManagement;
    @FXML private Button btnOrderManagement;
    @FXML private Button btnCategoryManagement;
    @FXML private Button btnSignOut;

    public void initialize() {
        loadDashboardData();
    }

//    public void initializeUserData(User user) {
//        lblUserName.setText(user.getFullName());
//        if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
//            imgAvatar.setImage(new Image(getClass().getResourceAsStream("/image/avt.jpg")));
//        }
//    }

    private void loadDashboardData() {
        // Load summary data (for cards and top categories)
        loadSummaryData();
        // Load sales by month (for LineChart)
        loadSalesByMonth();
    }

    private void loadSummaryData() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8081/api/admin/stats/summary");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + getAuthToken());

                if (conn.getResponseCode() == 200) {
                    JsonNode response = new ObjectMapper().readTree(conn.getInputStream());
                    JsonNode data = response.path("data");

                    // Format numbers
                    DecimalFormat formatter = new DecimalFormat("#,###");

                    // Update Summary Stats
                    Platform.runLater(() -> {
                        lblTotalSales.setText(formatter.format(data.path("totalSales").asDouble()) + " VND");
                        lblTotalOrders.setText(String.valueOf(data.path("totalOrders").asInt()));
                        lblTotalProducts.setText(String.valueOf(data.path("totalProducts").asInt()));
                        lblTotalCustomers.setText(String.valueOf(data.path("totalCustomers").asInt()));
                    });

                    // Update Top Categories PieChart
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    JsonNode topCategories = data.path("topCategories");
                    topCategories.forEach(category -> {
                        pieData.add(new PieChart.Data(
                                category.path("categoryName").asText(),
                                category.path("sales").asDouble()
                        ));
                    });

                    Platform.runLater(() -> {
                        topCategoriesChart.setData(pieData);
                        topCategoriesChart.setTitle("Doanh thu theo danh mục");
                    });

                } else {
                    Platform.runLater(() -> {
                        try {
                            showError("Lỗi khi lấy dữ liệu tổng quan: " + conn.getResponseCode());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu tổng quan: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSalesByMonth() {
        new Thread(() -> {
            try {
                // Cập nhật URL để khớp với endpoint trong Postman
                URL url = new URL("http://localhost:8081/api/admin/stats/sales?period=month");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + getAuthToken());

                if (conn.getResponseCode() == 200) {
                    JsonNode response = new ObjectMapper().readTree(conn.getInputStream());
                    System.out.println("API Response (Sales by Month): " + response.toString()); // In JSON để kiểm tra
                    JsonNode data = response.path("data");

                    // Get labels and salesData
                    JsonNode labels = data.path("labels");
                    JsonNode salesData = data.path("salesData");

                    // Kiểm tra dữ liệu
                    if (labels.size() == 0 || salesData.size() == 0) {
                        Platform.runLater(() -> showError("Không có dữ liệu doanh thu theo tháng."));
                        return;
                    }

                    // Create LineChart series
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Doanh thu theo tháng");

                    int minSize = Math.min(labels.size(), salesData.size());
                    for (int i = 0; i < minSize; i++) {
                        String label = labels.get(i).asText();
                        double sales = salesData.get(i).asDouble();
                        series.getData().add(new XYChart.Data<>(label, sales));
                    }

                    Platform.runLater(() -> {
                        salesChart.getData().clear();
                        salesChart.getData().add(series);
                        salesChart.setTitle("Doanh thu theo tháng");
                    });

                } else {
                    Platform.runLater(() -> {
                        try {
                            showError("Lỗi khi lấy dữ liệu doanh thu theo tháng: " + conn.getResponseCode());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu doanh thu theo tháng: " + e.getMessage()));
            }
        }).start();
    }

    private String getAuthToken() {
        return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ2ODMwMDU3LCJleHAiOjE3NDgyNzAwNTd9.rHSbPhS6PGyv-6sg6au8DfOeGCBWPMNwOfMm1I0wVA4";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
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
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        fadeOut.play();
    }

    @FXML
    public void handleProductManagement() {
        Stage stage = (Stage) btnProductManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
    }

    @FXML
    public void handleUserManagement() {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleOrderManagement() {
        Stage stage = (Stage) btnOrderManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/OrderManagement.fxml", "Quản lý đơn hàng");
    }

    @FXML
    public void handleCategoryManagement() {
        Stage stage = (Stage) btnCategoryManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/CategoryManagement.fxml", "Quản lý danh mục sản phẩm");
    }
}