package com.javaadv.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.Model.Product;
import com.javaadv.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ProductController implements Initializable {

    // FXML Components - Navigation
    @FXML private BorderPane mainContent;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblUserName;
    @FXML private Label lblUserPosition;
    @FXML private Button btnOverview;
    @FXML private Button btnUserManagement;
    @FXML private Button btnProductManagement;
    @FXML private Button btnOrderManagement;
    @FXML private Button btnCategoryManagement;
    @FXML private Button btnSignOut;

    // FXML Components - Table and Controls
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private Button btnAddProduct;
    @FXML private Button btnRefresh;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, List<String>> colImage;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colSize;
    @FXML private TableColumn<Product, Double> colDiscount;
    @FXML private TableColumn<Product, String> colStatus;

    // FXML Components - Detail Form
    @FXML private Label formTitle;
    @FXML private ImageView productImageView;
    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField discountField;
    @FXML private TextField imageUrlField;
    @FXML private TextField sizeField;
    @FXML private TextArea descArea;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    // Data and Configuration
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String BASE_URL = "http://localhost:8081/api/admin/products";
    private final String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ2ODI4Mzk3LCJleHAiOjE3NDgyNjgzOTd9.WDA-xfxLOuptav0WLNW0dl-wcU2Cn3EtGwrmz9yc61c";

    private Product selectedProduct;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupCategoryFilter();
        setupForm();
        loadProducts();
        setupImageUrlListener();
    }

    // ================ TABLE SETUP ================
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));

        colPrice.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        colImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colImage.setCellFactory(col -> new TableCell<Product, List<String>>() {
            private final ImageView imageView = new ImageView();
            private final Label errorLabel = new Label("?");
            {
                errorLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #dc3545; -fx-font-weight: bold;");
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(List<String> imageUrls, boolean empty) {
                super.updateItem(imageUrls, empty);
                if (empty || imageUrls == null || imageUrls.isEmpty()) {
                    setGraphic(errorLabel);
                } else {
                    try {
                        String firstUrl = imageUrls.get(0);
                        Image image = new Image(firstUrl, 40, 40, true, true, true);
                        if (image.isError()) {
                            setGraphic(errorLabel);
                        } else {
                            imageView.setImage(image);
                            setGraphic(imageView);
                        }
                    } catch (Exception e) {
                        setGraphic(errorLabel);
                    }
                }
            }
        });

        colSize.setCellValueFactory(cellData -> {
            List<Double> sizes = cellData.getValue().getSize();
            String text = (sizes != null && !sizes.isEmpty())
                    ? sizes.stream().map(Object::toString).collect(Collectors.joining(", "))
                    : "N/A";
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        colStatus.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStockQuantity() > 0 ? "Còn hàng" : "Hết hàng";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        productTable.setItems(productList);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldText, newText) -> filterProducts());
    }

    private void setupCategoryFilter() {
        cmbCategory.getItems().add("Tất cả danh mục");
        loadCategories();
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> filterProducts());
    }

    private void setupForm() {
        resetForm();
        setupFormValidation();
    }

    private void setupImageUrlListener() {
//        imageUrlField.textProperty().addListener((obs, oldText, newText) -> {
//            loadProductImage(newText);
//        });
    }

    private void setupFormValidation() {
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.length() > 100) {
                nameField.setText(oldText);
            }
        });

        priceField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*\\.?\\d*")) {
                priceField.setText(oldText);
            }
        });

        stockField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                stockField.setText(oldText);
            }
        });

        discountField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*\\.?\\d*")) {
                discountField.setText(oldText);
            }
        });
    }

    // ================ DATA LOADING ================
    private void loadProducts() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/list?pageNo=0");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonNode root = objectMapper.readTree(response.toString());
                    JsonNode items = root.path("data").path("items");
                    List<Product> products = objectMapper.convertValue(items, new TypeReference<List<Product>>(){});
                    Platform.runLater(() -> {
                        productList.setAll(products);
                        updateTableInfo();
                    });
                } else {
                    Platform.runLater(() -> {
                        try {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu. Mã lỗi: " + conn.getResponseCode());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    private void loadCategories() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/categories");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    List<String> categories = objectMapper.readValue(response.toString(), new TypeReference<List<String>>(){});
                    Platform.runLater(() -> {
                        cmbCategory.getItems().addAll(categories);
                        categoryCombo.getItems().addAll(categories);
                        cmbCategory.setValue("Tất cả danh mục");
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh mục: " + e.getMessage()));
            }
        }).start();
    }

    private void loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            System.out.println("Image URL is null or empty for product ID: " + (selectedProduct != null ? selectedProduct.getId() : "N/A"));
            Platform.runLater(() -> productImageView.setImage(null));
            return;
        }

        System.out.println("Attempting to load image from: " + imageUrl);
        try {
            Image image = new Image(imageUrl, 150, 150, true, true, true);
            if (image.isError()) {
                System.out.println("Error loading image from " + imageUrl + ". Exception: " + image.getException());
                Platform.runLater(() -> productImageView.setImage(null));
            } else {
                System.out.println("Image loaded successfully from " + imageUrl);
                Platform.runLater(() -> productImageView.setImage(image));
            }
        } catch (Exception e) {
            System.out.println("Exception loading image from " + imageUrl + ": " + e.getMessage());
            Platform.runLater(() -> productImageView.setImage(null));
        }
    }

    // ================ FILTERING ================
    private void filterProducts() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        String selectedCategory = cmbCategory.getValue() == null || "Tất cả danh mục".equals(cmbCategory.getValue()) ? "" : cmbCategory.getValue();

        ObservableList<Product> filtered = productList.filtered(product -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    product.getName().toLowerCase().contains(searchText) ||
                    String.valueOf(product.getId()).contains(searchText) ||
                    (product.getDescription() != null && product.getDescription().toLowerCase().contains(searchText));
            boolean matchesCategory = selectedCategory.isEmpty() ||
                    product.getCategoryName().equals(selectedCategory);
            return matchesSearch && matchesCategory;
        });
        productTable.setItems(filtered);
        updateTableInfo();
    }

    private void updateTableInfo() {
        Platform.runLater(() -> {
            int totalItems = productTable.getItems().size();
        });
    }

    // ================ EVENT HANDLERS ================
    @FXML
    private void handleTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Product product = productTable.getSelectionModel().getSelectedItem();
            if (product != null) {
                loadProductToForm(product);
            }
        } else if (event.getClickCount() == 1) {
            Product product = productTable.getSelectionModel().getSelectedItem();
            if (product != null) {
                loadProductToForm(product);
            }
        }
    }

    @FXML
    private void handleAddProduct() {
        resetForm();
        isEditMode = false;
        selectedProduct = null;
        formTitle.setText("Thêm sản phẩm mới");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);
        nameField.requestFocus();
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        Map<String, Object> data = createProductData();

        if (isEditMode && selectedProduct != null) {
            updateProduct(selectedProduct.getId(), data);
        } else {
            createProduct(data);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedProduct != null && isEditMode) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Bạn có chắc chắn muốn xóa sản phẩm '" + selectedProduct.getName() + "'?\nThao tác này không thể hoàn tác!");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteProduct(selectedProduct.getId());
            }
        }
    }

    @FXML
    private void handleClear() {
        resetForm();
        productTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
        resetForm();
    }

    // ================ FORM OPERATIONS ================
    private void loadProductToForm(Product product) {
        selectedProduct = product;
        isEditMode = true;

        formTitle.setText("Chi tiết sản phẩm - " + product.getName());
        saveBtn.setText("Cập nhật");
        deleteBtn.setVisible(true);

        idLabel.setText(String.valueOf(product.getId()));
        nameField.setText(product.getName());
        categoryCombo.setValue(product.getCategoryName());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStockQuantity()));
        discountField.setText(String.valueOf(product.getDiscount()));

        // Lấy và hiển thị URL hình ảnh đầu tiên
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            String firstImageUrl = product.getImageUrl().get(0); // Lấy URL đầu tiên
            System.out.println("Setting first image URL for product ID " + product.getId() + ": " + firstImageUrl);
            imageUrlField.setText(firstImageUrl); // Gán chỉ URL đầu tiên
            loadProductImage(firstImageUrl); // Gọi loadProductImage với URL đầu tiên
        } else {
            System.out.println("No image URL found for product: " + product.getId());
            imageUrlField.clear();
            productImageView.setImage(null);
        }

        if (product.getSize() != null && !product.getSize().isEmpty()) {
            String sizes = product.getSize().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            sizeField.setText(sizes);
        } else {
            sizeField.clear();
        }

        descArea.setText(product.getDescription() != null ? product.getDescription() : "");

        String status = product.getStockQuantity() > 0 ? "Còn hàng" : "Hết hàng";
        statusLabel.setText(status);
        statusLabel.setStyle("-fx-text-fill: " + (product.getStockQuantity() > 0 ? "#28a745" : "#dc3545") + "; -fx-font-weight: bold;");
    }
    private void resetForm() {
        selectedProduct = null;
        isEditMode = false;

        formTitle.setText("Thông tin sản phẩm");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);

        idLabel.setText("Tự sinh");
        nameField.clear();
        categoryCombo.setValue(null);
        priceField.setText("0");
        stockField.setText("0");
        discountField.setText("0");
        imageUrlField.clear();
        sizeField.clear();
        descArea.clear();
        statusLabel.setText("Chưa cập nhật");
        statusLabel.setStyle("-fx-text-fill: #6c757d;");

        productImageView.setImage(null);
    }

    private Map<String, Object> createProductData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", nameField.getText().trim());
        data.put("categoryName", categoryCombo.getValue());
        data.put("price", Double.parseDouble(priceField.getText()));
        data.put("stockQuantity", Integer.parseInt(stockField.getText()));
        data.put("discount", Double.parseDouble(discountField.getText()));
        data.put("description", descArea.getText().trim());

        if (!imageUrlField.getText().trim().isEmpty()) {
            data.put("imageUrl", Arrays.asList(imageUrlField.getText().trim()));
        }

        if (!sizeField.getText().trim().isEmpty()) {
            List<Double> sizes = Arrays.stream(sizeField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            data.put("size", sizes);
        }

        return data;
    }

    // ================ VALIDATION ================
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên sản phẩm không được để trống!");
            nameField.requestFocus();
            return false;
        }

        if (nameField.getText().trim().length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên sản phẩm phải có ít nhất 3 ký tự!");
            nameField.requestFocus();
            return false;
        }

        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn một danh mục!");
            categoryCombo.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            if (price < 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm không được âm!");
                priceField.requestFocus();
                return false;
            }
            if (price > 999999999) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm quá lớn!");
                priceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm phải là số hợp lệ!");
            priceField.requestFocus();
            return false;
        }

        try {
            int stock = Integer.parseInt(stockField.getText());
            if (stock < 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng tồn không được âm!");
                stockField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng tồn phải là số nguyên hợp lệ!");
            stockField.requestFocus();
            return false;
        }

        try {
            double discount = Double.parseDouble(discountField.getText());
            if (discount < 0 || discount > 100) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giảm giá phải từ 0 đến 100%!");
                discountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giảm giá phải là số hợp lệ!");
            discountField.requestFocus();
            return false;
        }

        if (!sizeField.getText().trim().isEmpty()) {
            try {
                Arrays.stream(sizeField.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(Double::parseDouble);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Kích thước phải là các số, cách nhau bằng dấu phẩy!");
                sizeField.requestFocus();
                return false;
            }
        }

        String imageUrl = imageUrlField.getText().trim();
        if (!imageUrl.isEmpty() && !isValidUrl(imageUrl)) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "URL hình ảnh có vẻ không hợp lệ!");
        }

        return true;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    // ================ API OPERATIONS ================
    private void createProduct(Map<String, Object> data) {
        executeApiCall("POST", BASE_URL, data, "Đã thêm sản phẩm '" + data.get("name") + "' thành công!");
    }

    private void updateProduct(int id, Map<String, Object> data) {
        executeApiCall("PUT", BASE_URL + "/update/" + id, data, "Đã cập nhật sản phẩm '" + data.get("name") + "' thành công!");
    }

    private void deleteProduct(int id) {
        executeApiCall("DELETE", BASE_URL + "/" + id, null, "Đã xóa sản phẩm thành công!");
    }

    private void executeApiCall(String method, String urlStr, Map<String, Object> data, String successMessage) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                if (data != null) {
                    conn.setRequestProperty("Content-Type", "application/json");
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
                        responseMessage = "Lỗi không xác định";
                    }
                }

                conn.disconnect();

                final String finalResponseMessage = responseMessage;
                Platform.runLater(() -> {
                    if (code == 200 || code == 201) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
                        loadProducts();
                        if ("DELETE".equals(method)) {
                            resetForm();
                        }
                    } else {
                        String errorMsg = "Thao tác thất bại. Mã lỗi: " + code;
                        if (!finalResponseMessage.isEmpty()) {
                            errorMsg += "\nChi tiết: " + finalResponseMessage;
                        }
                        showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    // ================ UTILITY METHODS ================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        String bgColor = "#d4edda";
        if (type == Alert.AlertType.ERROR) bgColor = "#f8d7da";
        else if (type == Alert.AlertType.WARNING) bgColor = "#fff3cd";

        alert.getDialogPane().setStyle("-fx-background-color: " + bgColor + "; -fx-border-radius: 5;");
        alert.showAndWait();
    }

    // ================ NAVIGATION HANDLERS ================
    @FXML
    public void handleOverview(ActionEvent event) {
        SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/Dashboard.fxml", "Tổng quan");
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleOrderManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/OrderManagement.fxml", "Quản lý đơn hàng");
    }

    @FXML
    public void handleCategoryManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/CategoryManagement.fxml", "Quản lý danh mục");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận đăng xuất");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn đăng xuất?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/Login.fxml", "Đăng nhập");
        }
    }
}