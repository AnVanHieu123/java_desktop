package com.javaadv.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.SceneManager;
import com.javaadv.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    // Cấu hình API
    private static final String API_BASE_URL = "http://localhost:8081/api/admin/stats/";
    private static final String API_SUMMARY = "summary";
    private static final String API_SALES = "sales";
    private static final String API_SALES_BY_CATEGORY = "sales-by-category";
    private static final String API_ORDERS_STATUS = "orders-status";

    // FXML Components
    @FXML private BorderPane mainContent;
    @FXML private Label lblUserName;
    @FXML private ImageView imgAvatar;

    @FXML private Label lblTotalSales, lblTotalOrders, lblTotalProducts, lblTotalCustomers;
    @FXML private Label lblCurrentMonthSales, lblTopCategoryName, lblDateFilterResult, lblDateFilterSales;

    @FXML private PieChart pieChart;
    @FXML private VBox chartContainer;
    @FXML private VBox apiSummaryCard, apiSalesByMonthCard, apiSalesByCategoryCard, apiOrdersByDateCard;

    @FXML private Button btnUserManagement, btnProductManagement, btnOrderManagement, btnCategoryManagement;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Button btnCloseChart;
    @FXML private HBox dateFilterControls;
    @FXML private Label lblChartTitle;

    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void initialize() {
        // Thiết lập giá trị mặc định
        setupDefaultValues();

        // Hiển thị tên người dùng
        setupUserInfo("Admin");

        // Tải dữ liệu ban đầu
        loadAllData();
    }

    private void setupDefaultValues() {
        lblTotalSales.setText("0 VND");
        lblCurrentMonthSales.setText("0 VND");
        lblTopCategoryName.setText("Đang tải...");
        lblDateFilterResult.setText("0");
        lblDateFilterSales.setText("0 VND");

        // Thiết lập ngày mặc định
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());
    }

    private void loadAllData() {
        loadSummaryData();
        loadSalesData();
        loadCategoryData();
        loadOrderStatusData();
    }

    //------------------------------- API CALLS -------------------------------

    private JsonNode callApi(String endpoint) throws IOException {
        HttpURLConnection conn = null;
        String token = SessionManager.getInstance().getAccessToken();
        try {
            URL url = new URL(API_BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 401 || responseCode == 403) {
                    throw new IOException("Token hết hạn hoặc không có quyền truy cập. Vui lòng đăng nhập lại.");
                } else {
                    throw new IOException("Lỗi API: " + responseCode);
                }
            }

            return objectMapper.readTree(conn.getInputStream()).path("data");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private JsonNode getSummaryData() throws IOException {
        return callApi(API_SUMMARY);
    }

    private JsonNode getSalesData(String period) throws IOException {
        return callApi(API_SALES + "?period=" + period);
    }

    private JsonNode getSalesByCategory() throws IOException {
        return callApi(API_SALES_BY_CATEGORY);
    }

    private JsonNode getOrdersByStatus() throws IOException {
        return callApi(API_ORDERS_STATUS);
    }

    private JsonNode getOrdersByDateRange(LocalDate startDate, LocalDate endDate) throws IOException {
        String startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return callApi(API_ORDERS_STATUS + "?startDate=" + startDateStr + "&endDate=" + endDateStr);
    }

    //------------------------------- DATA LOADING -------------------------------

    private void loadSummaryData() {
        new Thread(() -> {
            try {
                JsonNode data = getSummaryData();
                if (data.isMissingNode()) {
                    Platform.runLater(() -> showError("Dữ liệu tổng quan không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    lblTotalSales.setText(formatter.format(getDoubleValue(data, "totalSales")) + " VND");
//                    lblTotalOrders.setText(String.valueOf(getIntValue(data, "totalOrders")));
//                    lblTotalProducts.setText(String.valueOf(getIntValue(data, "totalProducts")));
//                    lblTotalCustomers.setText(String.valueOf(getIntValue(data, "totalCustomers")));
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu tổng quan: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSalesData() {
        new Thread(() -> {
            try {
                JsonNode data = getSalesData("monthly");
                if (data.isMissingNode()) {
                    Platform.runLater(() -> showError("Dữ liệu doanh thu không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    JsonNode salesData = data.path("salesData");
                    if (salesData.isArray() && salesData.size() > 0) {
                        double currentMonthSales = salesData.get(0).asDouble(0);
                        lblCurrentMonthSales.setText(formatter.format(currentMonthSales) + " VND");
                    } else {
                        lblCurrentMonthSales.setText("0 VND");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu doanh thu: " + e.getMessage()));
            }
        }).start();
    }

    private void loadCategoryData() {
        new Thread(() -> {
            try {
                JsonNode data = getSalesByCategory();
                if (data.isMissingNode() || !data.isArray()) {
                    Platform.runLater(() -> showError("Dữ liệu danh mục không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    if (data.size() > 0) {
                        JsonNode topCategory = data.get(0);
                        lblTopCategoryName.setText(getStringValue(topCategory, "categoryName"));
                    } else {
                        lblTopCategoryName.setText("Chưa có dữ liệu");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu danh mục: " + e.getMessage()));
            }
        }).start();
    }

    private void loadOrderStatusData() {
        new Thread(() -> {
            try {
                JsonNode data = getOrdersByStatus();
                if (data.isMissingNode() || !data.isArray()) {
                    Platform.runLater(() -> showError("Dữ liệu đơn hàng không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    int totalOrders = 0;
                    for (JsonNode status : data) {
                        totalOrders += getIntValue(status, "count");
                    }
                    lblDateFilterResult.setText(String.valueOf(totalOrders));
                    lblDateFilterSales.setText("N/A"); // API không trả về doanh thu
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải dữ liệu đơn hàng: " + e.getMessage()));
            }
        }).start();
    }

    //------------------------------- CHART LOADING -------------------------------

    @FXML
    private void handleApiCardClick(MouseEvent event) {
        if (event.getClickCount() != 2) {
            return;
        }

        Object source = event.getSource();
        System.out.println("Source clicked: " + source); // Debug để kiểm tra nguồn sự kiện

        // Kiểm tra xem source có phải là một trong các thẻ không
        if (source == apiSummaryCard) {
            System.out.println("Summary card clicked");
            showSummaryChart();
        } else if (source == apiSalesByMonthCard) {
            System.out.println("Sales by month card clicked");
            showSalesByMonthChart();
        } else if (source == apiSalesByCategoryCard) {
            System.out.println("Sales by category card clicked");
            showSalesByCategoryChart();
        } else if (source == apiOrdersByDateCard) {
            System.out.println("Orders by date card clicked");
            showOrdersByDateChart();
        } else {
            showError("Không thể xác định thẻ được nhấp: " + source.toString());
        }
    }

    private void showPieChartInNewWindow(String title, ObservableList<PieChart.Data> pieData, double totalValue, String valueFormat) {
        Stage stage = new Stage();
        stage.setTitle(title);

        PieChart chart = new PieChart(pieData);
        chart.setTitle(title);
        chart.setPrefSize(800, 600);
        chart.setLegendVisible(false); // Tắt chú thích mặc định
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(15);
        chart.setStartAngle(90);

        // Tùy chỉnh giao diện biểu đồ
        chart.setStyle(
                "-fx-font-family: 'Segoe UI'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-pie-label-visible: true; " +
                        "-fx-title-font: 20px 'Segoe UI Semibold'; " +
                        "-fx-background-color: transparent;"
        );

        // Bảng màu mới
        String[] colors = {
                "#FF6F61", "#6B5B95", "#88B04B", "#F7CAC9", "#92A8D1", "#F4A261"
        };

        // Tính toán và chuẩn hóa dữ liệu để tránh chênh lệch quá lớn
        ObservableList<PieChart.Data> normalizedPieData = FXCollections.observableArrayList();
        double maxValue = pieData.stream().mapToDouble(PieChart.Data::getPieValue).max().orElse(1.0);
        for (PieChart.Data slice : pieData) {
            double normalizedValue = (slice.getPieValue() / maxValue) * 100; // Chuyển thành tỷ lệ phần trăm
            if (normalizedValue > 1.0) { // Chỉ hiển thị các lát có tỷ lệ đáng kể
                normalizedPieData.add(new PieChart.Data(slice.getName(), normalizedValue));
            }
        }

        // Tạo chú thích tùy chỉnh
        VBox legendBox = new VBox(5);
        legendBox.setStyle(
                "-fx-padding: 10; " +
                        "-fx-background-color: #F7FAFC; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8;"
        );

        // Thêm chart và chú thích vào scene
        VBox root = new VBox(10, chart, legendBox);
        root.setStyle(
                "-fx-padding: 20; " +
                        "-fx-background-color: #F7FAFC; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12;"
        );
        Scene scene = new Scene(root, 820, 700); // Tăng chiều cao để chứa chú thích
        stage.setScene(scene);
        stage.show();

        // Đợi cho chart render xong, sau đó tùy chỉnh node và chú thích
        Platform.runLater(() -> {
            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(5.0);
            dropShadow.setOffsetX(2.0);
            dropShadow.setOffsetY(2.0);

            int colorIndex = 0;
            for (PieChart.Data slice : normalizedPieData) {
                Node node = slice.getNode();
                if (node != null) {
                    String color = colors[colorIndex % colors.length];
                    node.setStyle(
                            "-fx-pie-color: " + color + "; " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-color: #ffffff;"
                    );
                    node.setEffect(dropShadow);

                    // Tùy chỉnh nhãn trên lát (hiển thị giá trị gốc và tỷ lệ phần trăm)
                    double percentage = totalValue > 0 ? (slice.getPieValue() / totalValue) * 100 : 0;
                    String labelText;
                    if (valueFormat.equals("VND")) {
                        labelText = formatter.format((slice.getPieValue() / 100) * maxValue) + " VND\n" + String.format("%.1f%%", percentage);
                    } else if (valueFormat.equals("Count")) {
                        labelText = String.format("%.0f", (slice.getPieValue() / 100) * maxValue) + "\n" + String.format("%.1f%%", percentage);
                    } else {
                        labelText = String.format("%.0f", (slice.getPieValue() / 100) * maxValue) + "\n" + String.format("%.1f%%", percentage);
                    }
                    slice.setName(slice.getName() + "\n" + labelText);

                    // Thêm mục vào chú thích tùy chỉnh
                    HBox legendItem = new HBox(10);
                    Text colorBox = new Text("■");
                    colorBox.setStyle(
                            "-fx-fill: " + color + "; " +
                                    "-fx-font-size: 16px;"
                    );
                    String legendText;
                    if (valueFormat.equals("VND")) {
                        legendText = slice.getName().split("\n")[0] + ": " + formatter.format((slice.getPieValue() / 100) * maxValue) + " VND (" + String.format("%.1f%%", percentage) + ")";
                    } else if (valueFormat.equals("Count")) {
                        legendText = slice.getName().split("\n")[0] + ": " + String.format("%.0f", (slice.getPieValue() / 100) * maxValue) + " (" + String.format("%.1f%%", percentage) + ")";
                    } else {
                        legendText = slice.getName().split("\n")[0] + ": " + String.format("%.0f", (slice.getPieValue() / 100) * maxValue) + " (" + String.format("%.1f%%", percentage) + ")";
                    }
                    Text legendLabel = new Text(legendText);
                    legendLabel.setStyle(
                            "-fx-font-family: 'Segoe UI'; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-fill: #4A5568;"
                    );
                    legendItem.getChildren().addAll(colorBox, legendLabel);
                    legendBox.getChildren().add(legendItem);

                    // Tùy chỉnh tooltip
                    Tooltip tooltip = new Tooltip(slice.getName());
                    tooltip.setStyle(
                            "-fx-font-family: 'Segoe UI'; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-background-color: #2D3748; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-padding: 8;"
                    );
                    Tooltip.install(node, tooltip);

                    // Hiệu ứng hover
                    node.setOnMouseEntered(e -> node.setStyle(
                            "-fx-pie-color: " + color + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-color: #ffffff; " +
                                    "-fx-scale-x: 1.05; " +
                                    "-fx-scale-y: 1.05;"
                    ));
                    node.setOnMouseExited(e -> node.setStyle(
                            "-fx-pie-color: " + color + "; " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-color: #ffffff;"
                    ));

                    colorIndex++;
                }
            }
        });
    }

    private void showSummaryChart() {
        new Thread(() -> {
            try {
                JsonNode data = getSummaryData();
                if (data.isMissingNode()) {
                    Platform.runLater(() -> showError("Dữ liệu tổng quan không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    double totalValue = 0;

                    double totalSales = getDoubleValue(data, "totalSales");
                    int totalOrders = getIntValue(data, "totalOrders");
                    int totalProducts = getIntValue(data, "totalProducts");
                    int totalCustomers = getIntValue(data, "totalCustomers");

                    pieData.add(new PieChart.Data("Doanh thu (VND)", totalSales));
                    pieData.add(new PieChart.Data("Đơn hàng", totalOrders));
                    pieData.add(new PieChart.Data("Sản phẩm", totalProducts));
                    pieData.add(new PieChart.Data("Khách hàng", totalCustomers));

                    totalValue = totalSales + totalOrders + totalProducts + totalCustomers;

                    showPieChartInNewWindow("Tổng quan doanh thu", pieData, totalValue, "Mixed");
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải biểu đồ tổng quan: " + e.getMessage()));
            }
        }).start();
    }

    private void showSalesByMonthChart() {
        new Thread(() -> {
            try {
                JsonNode data = getSalesData("monthly");
                if (data.isMissingNode()) {
                    Platform.runLater(() -> showError("Dữ liệu doanh thu không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    JsonNode labels = data.path("labels");
                    JsonNode salesData = data.path("salesData");
                    JsonNode orderCounts = data.path("orderCounts");

                    if (!labels.isMissingNode() && !salesData.isMissingNode() && !orderCounts.isMissingNode() &&
                            labels.isArray() && salesData.isArray() && orderCounts.isArray()) {
                        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                        double totalValue = 0;

                        int size = Math.min(labels.size(), Math.min(salesData.size(), orderCounts.size()));
                        for (int i = 0; i < size; i++) {
                            String month = labels.get(i).asText("");
                            double sales = salesData.get(i).asDouble(0);
                            int orders = orderCounts.get(i).asInt(0);
                            if (!month.isEmpty() && sales > 0) {
                                pieData.add(new PieChart.Data(month + " (Doanh thu: " + formatter.format(sales) + " VND, Đơn: " + orders + ")", sales));
                                totalValue += sales;
                            }
                        }

                        if (pieData.isEmpty()) {
                            showError("Không có dữ liệu doanh thu hợp lệ để hiển thị.");
                            return;
                        }

                        showPieChartInNewWindow("Doanh thu theo tháng", pieData, totalValue, "VND");
                    } else {
                        showError("Dữ liệu doanh thu không hợp lệ: labels, salesData hoặc orderCounts không đúng định dạng.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải biểu đồ doanh thu: " + e.getMessage()));
            }
        }).start();
    }

    private void showSalesByCategoryChart() {
        new Thread(() -> {
            try {
                JsonNode data = getSalesByCategory();
                if (data.isMissingNode() || !data.isArray()) {
                    Platform.runLater(() -> showError("Dữ liệu danh mục không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    double totalValue = 0;

                    for (JsonNode category : data) {
                        String categoryName = getStringValue(category, "categoryName");
                        double sales = getDoubleValue(category, "totalSales");
                        if (!categoryName.isEmpty() && sales > 0) {
                            pieData.add(new PieChart.Data(categoryName + " (Doanh thu: " + formatter.format(sales) + " VND)", sales));
                            totalValue += sales;
                        }
                    }

                    if (pieData.isEmpty()) {
                        showError("Không có dữ liệu danh mục hợp lệ để hiển thị.");
                        return;
                    }

                    showPieChartInNewWindow("Doanh thu theo danh mục", pieData, totalValue, "VND");
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải biểu đồ danh mục: " + e.getMessage()));
            }
        }).start();
    }

    private void showOrdersByDateChart() {
        new Thread(() -> {
            try {
                JsonNode data = getOrdersByStatus();
                if (data.isMissingNode() || !data.isArray()) {
                    Platform.runLater(() -> showError("Dữ liệu đơn hàng không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    double totalValue = 0;

                    for (JsonNode status : data) {
                        String statusName = getStringValue(status, "status");
                        int count = getIntValue(status, "count");
                        if (!statusName.isEmpty() && count > 0) {
                            pieData.add(new PieChart.Data(statusName + " (Số lượng: " + count + ")", count));
                            totalValue += count;
                        }
                    }

                    if (pieData.isEmpty()) {
                        showError("Không có dữ liệu đơn hàng hợp lệ để hiển thị.");
                        return;
                    }

                    showPieChartInNewWindow("Đơn hàng theo trạng thái", pieData, totalValue, "Count");
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi tải biểu đồ đơn hàng: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDateFilter(ActionEvent event) {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showError("Vui lòng chọn cả ngày bắt đầu và ngày kết thúc.");
            return;
        }

        if (end.isBefore(start)) {
            showError("Ngày kết thúc phải sau ngày bắt đầu.");
            return;
        }

        new Thread(() -> {
            try {
                JsonNode data = getOrdersByDateRange(start, end);
                if (data.isMissingNode() || !data.isArray()) {
                    Platform.runLater(() -> showError("Dữ liệu đơn hàng không khả dụng."));
                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                    double totalValue = 0;
                    int totalOrders = 0;

                    for (JsonNode status : data) {
                        String statusName = getStringValue(status, "status");
                        int count = getIntValue(status, "count");
                        if (!statusName.isEmpty() && count > 0) {
                            pieData.add(new PieChart.Data(statusName + " (Số lượng: " + count + ")", count));
                            totalValue += count;
                            totalOrders += count;
                        }
                    }

                    lblDateFilterResult.setText(String.valueOf(totalOrders));
                    lblDateFilterSales.setText("N/A"); // API không trả về doanh thu

                    String formattedStartDate = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    String formattedEndDate = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    showPieChartInNewWindow("Đơn hàng từ " + formattedStartDate + " đến " + formattedEndDate, pieData, totalValue, "Count");
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Lỗi lọc dữ liệu: " + e.getMessage()));
            }
        }).start();
    }

    //------------------------------- UTILITY METHODS -------------------------------

    private double getDoubleValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isNull() || !fieldNode.isNumber() ? 0.0 : fieldNode.asDouble();
    }

    private int getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isNull() || !fieldNode.isNumber() ? 0 : fieldNode.asInt();
    }

    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isNull() || !fieldNode.isTextual() ? "" : fieldNode.asText();
    }

    @FXML
    private void handleCloseChart() {
        // Không cần vì biểu đồ hiển thị trong cửa sổ mới
    }

    public void setupUserInfo(String userName) {
        if (userName != null && !userName.isEmpty() && lblUserName != null) {
            lblUserName.setText(userName);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //------------------------------- NAVIGATION METHODS -------------------------------

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        Stage stage = (Stage) mainContent.getScene().getWindow();
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
    public void handleUserManagement() {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleProductManagement() {
        Stage stage = (Stage) btnProductManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
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