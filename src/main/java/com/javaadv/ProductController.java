package com.javaadv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.Model.Product;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ProductController implements Initializable {

    @FXML private Button btnUserManagement;
    @FXML private Button btnOrderManagement;
    @FXML private Button btnCategoryManagement;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtStock;
    @FXML private TextField txtPrice;
    @FXML private TextField txtImageUrl;
    @FXML private TextField txtDiscount;
    @FXML private TextField txtDescription;

    @FXML private TextField txtSalePrice;
    @FXML private TextField txtCategory;
    @FXML private TextField txtSize;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnStatistics;


    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Product selectedProduct = null;
    private final String BASE_URL = "http://localhost:8081/api/admin/products";
    private String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ2ODI4Mzk3LCJleHAiOjE3NDgyNjgzOTd9.WDA-xfxLOuptav0WLNW0dl-wcU2Cn3EtGwrmz9yc61c";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFocusListener();
        setupButtonActions();
        loadProductsFromAPI();
        setupSearchListener();
        btnStatistics.setOnAction(e -> showStatistics());
        productTable.setOnMouseClicked(event -> {
            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                txtDiscount.setText(String.valueOf(selectedProduct.getDiscount()));
            }
        });

    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
    }
    private void filterProducts(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            productTable.setItems(productList); // Hiển thị toàn bộ danh sách
            return;
        }

        String lowerCaseKeyword = keyword.toLowerCase();

        ObservableList<Product> filteredList = productList.filtered(product ->
                product.getName().toLowerCase().contains(lowerCaseKeyword) ||
                        String.valueOf(product.getId()).contains(lowerCaseKeyword) ||
                        (product.getCategoryName() != null &&
                                product.getCategoryName().toLowerCase().contains(lowerCaseKeyword)) ||
                        String.valueOf(product.getPrice()).contains(lowerCaseKeyword)
        );

        productTable.setItems(filteredList);
    }
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Product, Double> colDiscount = new TableColumn<>("Giảm giá (%)");
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));

        TableColumn<Product, String> colSize = new TableColumn<>("Kích cỡ");
        colSize.setCellValueFactory(cellData -> {
            List<Double> sizes = cellData.getValue().getSize();
            return new javafx.beans.property.SimpleStringProperty(
                    sizes != null && !sizes.isEmpty() ? sizes.stream().map(Object::toString).collect(Collectors.joining(", ")) : "Không có"
            );
        });

        TableColumn<Product, String> colCategory = new TableColumn<>("Danh mục");
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        if (!productTable.getColumns().contains(colDiscount)) productTable.getColumns().add(colDiscount);
        if (!productTable.getColumns().contains(colSize)) productTable.getColumns().add(colSize);
        if (!productTable.getColumns().contains(colCategory)) productTable.getColumns().add(colCategory);

        productTable.setItems(productList);
    }
    @FXML
    private void showStatistics() {
        int totalProducts = productList.size();
        double totalValue = 0;
        int totalQuantity = 0;

        for (Product p : productList) {
            totalQuantity += p.getStockQuantity();
            totalValue += p.getStockQuantity() * p.getPrice();
        }

        // Hiển thị thông tin thống kê
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thống kê sản phẩm");
        alert.setHeaderText("Thông tin tổng hợp:");
        alert.setContentText(
                "Tổng số sản phẩm: " + totalProducts +
                        "\nTổng số lượng tồn: " + totalQuantity +
                        "\nTổng giá trị tồn kho: " + String.format("%,.0f VNĐ", totalValue)
        );
        alert.showAndWait();

        showChart(); // Gọi hàm hiển thị biểu đồ nếu muốn
    }

    private void showChart() {
        if (productList == null || productList.isEmpty()) {
            showAlert("Thông báo", "Không có dữ liệu sản phẩm để hiển thị");
            return;
        }

        // 1. Tạo dữ liệu cho biểu đồ mức giá
        ObservableList<PieChart.Data> priceData = productList.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            double price = p.getPrice();
                            if (price < 50000) return "Dưới 50k";
                            else if (price < 100000) return "50k-100k";
                            else if (price < 200000) return "100k-200k";
                            else return "Trên 200k";
                        },
                        Collectors.summingInt(p -> 1)
                ))
                .entrySet()
                .stream()
                .map(entry -> new PieChart.Data(
                        String.format("%s (%d)", entry.getKey(), entry.getValue()),
                        entry.getValue()
                ))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        // 2. Tạo dữ liệu cho biểu đồ tồn kho
        ObservableList<PieChart.Data> stockData = productList.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            int stock = p.getStockQuantity();
                            if (stock == 0) return "Hết hàng";
                            else if (stock < 5) return "Sắp hết (<5)";
                            else if (stock < 20) return "Tồn kho vừa (5-20)";
                            else return "Tồn kho nhiều (>20)";
                        },
                        Collectors.summingInt(p -> 1)
                ))
                .entrySet()
                .stream()
                .map(entry -> new PieChart.Data(
                        String.format("%s (%d)", entry.getKey(), entry.getValue()),
                        entry.getValue()
                ))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        // 3. Tạo biểu đồ
        PieChart priceChart = new PieChart(priceData);
        priceChart.setTitle("Phân bố theo mức giá");

        PieChart stockChart = new PieChart(stockData);
        stockChart.setTitle("Tình trạng tồn kho");

        // 4. Tuỳ chỉnh giao diện
        String[] priceColors = {"#2ecc71", "#3498db", "#f39c12", "#e74c3c"}; // Xanh lá, xanh dương, cam, đỏ
        String[] stockColors = {"#e74c3c", "#f39c12", "#3498db", "#2ecc71"}; // Đỏ, cam, xanh dương, xanh lá

        applyChartColors(priceChart, priceData, priceColors);
        applyChartColors(stockChart, stockData, stockColors);

        // 5. Bố cục giao diện
        HBox chartsBox = new HBox(20, priceChart, stockChart);
        chartsBox.setPadding(new Insets(15));
        chartsBox.setAlignment(Pos.CENTER);
        VBox root = new VBox(10); // spacing = 10
        root.getChildren().addAll(
                new Label("THỐNG KÊ SẢN PHẨM") {{
                    setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
                }},
                chartsBox
        );
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa;");

        // 6. Hiển thị cửa sổ
        Stage stage = new Stage();
        stage.setScene(new Scene(root, 900, 500));
        stage.setTitle("Thống kê sản phẩm");
        stage.show();
    }

    // Phương thức hỗ trợ: Áp dụng màu cho biểu đồ
    private void applyChartColors(PieChart chart, ObservableList<PieChart.Data> data, String[] colors) {
        for (int i = 0; i < data.size(); i++) {
            data.get(i).getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
        }
        chart.setStyle("-fx-font-size: 12px;");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
    }

    private void setupFocusListener() {
        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedProduct = newValue;
            if (selectedProduct != null) {
                populateFields(selectedProduct);
            } else {
                clearFields();
            }
        });
    }

    private void setupButtonActions() {
        btnAdd.setOnAction(this::handleAdd);
        btnUpdate.setOnAction(this::handleEdit);
        btnDelete.setOnAction(this::handleDelete);
    }



    private void loadProductsFromAPI() {
        List<Product> allProducts = new ArrayList<>();
        int currentPage = 0;
        int totalPages = 1;

        try {
            while (currentPage < totalPages) {
                URL url = new URL(BASE_URL + "/list?pageNo=" + currentPage);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("Server returned HTTP code: " + responseCode + " for page " + currentPage);
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                conn.disconnect();

                JsonNode rootNode = objectMapper.readTree(response.toString());
                JsonNode dataNode = rootNode.path("data");
                totalPages = dataNode.path("totalPage").asInt();
                JsonNode itemsNode = dataNode.path("items");
                List<Product> products = objectMapper.convertValue(itemsNode, new TypeReference<List<Product>>(){});

                allProducts.addAll(products);
                currentPage++;
            }

            Platform.runLater(() -> productList.setAll(allProducts));

        } catch (IOException e) {
            showAlert("Lỗi", "Không thể tải danh sách sản phẩm: " + e.getMessage());
        }
    }

    private void handleAdd(ActionEvent event) {
        try {
            // 1. Chuẩn bị request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", txtName.getText().trim());
            requestBody.put("price", Double.parseDouble(txtPrice.getText().replaceAll("[^\\d.]", "")));
            requestBody.put("stockQuantity", Integer.parseInt(txtStock.getText()));
            requestBody.put("categoryId", selectedProduct.getCategoryId(txtCategory.getText())); // Cần implement

            // Các trường không bắt buộc
            if (!txtDescription.getText().isEmpty()) {
                requestBody.put("description", txtDescription.getText());
            }
            if (!txtDiscount.getText().isEmpty()) {
                requestBody.put("discount", Double.parseDouble(txtDiscount.getText()));
            }
            if (!txtImageUrl.getText().isEmpty()) {
                requestBody.put("imageUrl", txtImageUrl.getText());
            }

            // 2. Gọi API
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL ).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " +accessToken);
            conn.setDoOutput(true);

            // 3. Gửi request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(requestBody));
            }

            // 4. Xử lý response
            if (conn.getResponseCode() == 200) {
                JsonNode response = objectMapper.readTree(conn.getInputStream());
                if (response.path("status").asInt() == 200) { // Kiểm tra status code trong response
                    showAlert("Thành công", "Thêm sản phẩm thành công!");
                    loadProductsFromAPI();
                    clearFields();
                }
            } else {
                JsonNode error = objectMapper.readTree(conn.getErrorStream());
                showAlert("Lỗi", error.path("message").asText());
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập đúng định dạng số cho giá/giảm giá/số lượng");
        } catch (Exception e) {
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    private void handleEdit(ActionEvent event) {
        if (selectedProduct == null) {
            showAlert("Lỗi", "Vui lòng chọn sản phẩm để sửa!");
            return;
        }

        try {
            selectedProduct.setId(Integer.parseInt(txtId.getText().trim()));
            selectedProduct.setName(txtName.getText().trim());
            selectedProduct.setPrice(Double.parseDouble(txtPrice.getText().replaceAll("[^\\d.]", "").trim()));
            selectedProduct.setStockQuantity(Integer.parseInt(txtStock.getText().trim()));
            selectedProduct.setCategoryName(txtCategory.getText().trim());
            selectedProduct.setSize(parseSizes(txtSize.getText().trim()));

            String json = objectMapper.writeValueAsString(selectedProduct);
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/update/" + selectedProduct.getId()).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " +accessToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                loadProductsFromAPI();
                clearFields();
                showAlert("Thành công", "Cập nhật sản phẩm thành công!");
            } else {
                showAlert("Lỗi", "Cập nhật sản phẩm thất bại: Mã " + responseCode);
            }
            conn.disconnect();

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập đúng định dạng số cho giá và số lượng!");
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi sửa sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            showAlert("Lỗi", "Vui lòng chọn sản phẩm cần xóa");
            return;
        }

        // Hiển thị hộp thoại xác nhận
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn xóa sản phẩm '" + selectedProduct.getName() + "'?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteProductFromAPI(selectedProduct.getId());
        }
    }

    private void deleteProductFromAPI(int productId) {
        try {
            // 1. Tạo URL với productId
            URL url = new URL("http://localhost:8081/api/admin/products/" + productId);

            // 2. Mở kết nối HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken  );
            conn.setRequestProperty("Content-Type", "application/json");

            // 3. Gửi request và nhận response
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Platform.runLater(() -> {
                    showAlert("Thành công", "Đã xóa sản phẩm thành công");
                    loadProductsFromAPI(); // Tải lại danh sách sau khi xóa
                });
            } else {
                // Đọc thông báo lỗi từ server
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()))) {
                    String errorResponse = errorReader.lines().collect(Collectors.joining());
                    Platform.runLater(() ->
                            showAlert("Lỗi", "Không thể xóa sản phẩm: " + errorResponse));
                }
            }
        } catch (IOException e) {
            Platform.runLater(() ->
                    showAlert("Lỗi kết nối", "Lỗi khi gọi API: " + e.getMessage()));
        }
    }


    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Product product = productTable.getSelectionModel().getSelectedItem();
            if (product != null) {
                // Hiển thị thông tin chi tiết (bạn đã có phần này)
                String sizeText = product.getSize() != null && !product.getSize().isEmpty() ?
                        product.getSize().stream().map(Object::toString).collect(Collectors.joining(", ")) : "Không có";
                String detailText = "ID: " + product.getId() +
                        "\nTên sản phẩm: " + (product.getName() != null ? product.getName() : "Không có") +
                        "\nDanh mục: " + (product.getCategoryName() != null ? product.getCategoryName() : "Không có") +
                        "\nGiá bán: " + (product.getPrice() != 0 ? currencyFormat.format(product.getPrice()) : "Không có") +
                        "\nSố lượng tồn: " + product.getStockQuantity() +
                        "\nKích cỡ: " + sizeText;

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Chi tiết sản phẩm");
                alert.setHeaderText(product.getName());
                alert.setContentText(detailText);
                alert.showAndWait();
            }
        }
    }

    private void populateFields(Product product) {
        Platform.runLater(() -> {
            txtId.setText(String.valueOf(product.getId()));
            txtName.setText(product.getName() != null ? product.getName() : "");

            // Định dạng giá tiền
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            txtPrice.setText(product.getPrice() != 0 ? currencyFormat.format(product.getPrice()) : "");

            txtStock.setText(String.valueOf(product.getStockQuantity()));
            txtCategory.setText(product.getCategoryName() != null ? product.getCategoryName() : "");
            txtSize.setText(product.getSize() != null && !product.getSize().isEmpty() ?
                    product.getSize().stream().map(Object::toString).collect(Collectors.joining(", ")) : "");
            txtDescription.setText(selectedProduct.getDescription()); // mô tả
            txtImageUrl.setText(String.valueOf(selectedProduct.getImageUrl())); // url ảnh

        });
    }
    @FXML
    private void clearFields() {
        txtId.setText("");
        txtName.setText("");
        txtPrice.setText("");
        txtCategory.setText("");
        txtStock.setText("");
        txtDescription.setText("");
        txtSize.setText("");
       txtStock.setText("");
       txtImageUrl.setText("");
        selectedProduct = null;
    }

    private List<Double> parseSizes(String sizeText) {
        List<Double> sizes = new ArrayList<>();
        if (sizeText != null && !sizeText.trim().isEmpty()) {
            String[] sizeArray = sizeText.split(",");
            for (String s : sizeArray) {
                try {
                    sizes.add(Double.parseDouble(s.trim()));
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu không parse được
                }
            }
        }
        return sizes;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleOverview(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Dashboard.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleProductManagement(ActionEvent event) {
        // Đã đang ở màn hình quản lý sản phẩm, không cần làm gì
    }

    @FXML
    public void handleLogout(ActionEvent event) {
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
    public void handleUserManagement(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleOrderManagement(ActionEvent event) {
        Stage stage = (Stage) btnOrderManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/OrderManagement.fxml", "Quản lý đơn hàng");
    }

    @FXML
    public void handleCategoryManagement(ActionEvent event) {
        Stage stage = (Stage) btnCategoryManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/CategoryManagement.fxml", "Quản lý danh mục");
    }



}