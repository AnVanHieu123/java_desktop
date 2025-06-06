package com.javaadv.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javaadv.Model.Category;
import com.javaadv.Model.Product;
import com.javaadv.SceneManager;
import com.javaadv.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static jdk.internal.classfile.impl.DirectCodeBuilder.build;

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
    @FXML private VBox detailForm;
    @FXML private Label formTitle;
    @FXML private ImageView productImageView;
    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField discountField;
    @FXML private TextField imageUrlField;
    @FXML private Button uploadButton;
    @FXML private TextField sizeField;
    @FXML private TextArea descArea;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    // Data and Configuration
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final String BASE_URL = "http://localhost:8081/api/admin/products";
    private final HttpClient httpClient;
    private Product selectedProduct;
    private boolean isEditMode = false;
    private File selectedImageFile;
    private Map<String, Integer> categoryNameToId = new HashMap<>();

    public ProductController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupCategoryFilter();
        setupForm();
        loadProducts();
        setupImageUrlListener();
        loadCategories();
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
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> filterProducts());
    }

    private void setupForm() {
        resetForm();
        setupFormValidation();
    }

    private void setupImageUrlListener() {
        imageUrlField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isEmpty()) {
                loadProductImage(newText);
            } else {
                productImageView.setImage(null);
            }
        });
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
            int retries = 1;
            List<Product> allProducts = new ArrayList<>();
            int pageNo = 0;
            int totalPages = 0; // Sẽ được cập nhật từ phản hồi API

            // Lấy trang đầu tiên để xác định totalPages
            for (int attempt = 1; attempt <= retries; attempt++) {
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
                            .uri(URI.create(BASE_URL + "/list?sortBy=id:desc&pageNo=" + pageNo))
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .GET()
                            .timeout(Duration.ofSeconds(5))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


                    if (response.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode data = root.path("data");
                        totalPages = data.path("totalPage").asInt(); // Lấy totalPage từ phản hồi
                        JsonNode items = data.path("items");
                        List<Product> products = objectMapper.convertValue(items, new TypeReference<List<Product>>(){});
                        allProducts.addAll(products);
                        break; // Thoát vòng lặp sau khi lấy trang đầu tiên
                    } else if (response.statusCode() == 401) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                            redirectToLogin();
                        });
                        return;
                    } else {
                        final String errorMessage = response.body();
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu. Mã lỗi: " + response.statusCode() + "\nChi tiết: " + errorMessage));
                        return;
                    }
                } catch (Exception e) {
                    if (attempt == retries) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server sau " + retries + " lần thử: " + e.getMessage()));
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

            // Lặp qua các trang còn lại (từ pageNo=1 đến totalPages-1)
            while (pageNo < totalPages - 1) {
                pageNo++;
                for (int attempt = 1; attempt <= retries; attempt++) {
                    try {
                        String token = SessionManager.getInstance().getAccessToken();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/list?sortBy=id:desc&pageNo=" + pageNo))
                                .header("Authorization", "Bearer " + token)
                                .header("Content-Type", "application/json")
                                .GET()
                                .timeout(Duration.ofSeconds(5))
                                .build();

                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        System.out.println("Phản hồi từ API - Trang " + pageNo + ": Status: " + response.statusCode() + ", Body: " + response.body());

                        if (response.statusCode() == 200) {
                            JsonNode root = objectMapper.readTree(response.body());
                            JsonNode data = root.path("data");
                            JsonNode items = data.path("items");
                            List<Product> products = objectMapper.convertValue(items, new TypeReference<List<Product>>(){});
                            allProducts.addAll(products);
                        } else if (response.statusCode() == 401) {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                                redirectToLogin();
                            });
                            return;
                        } else {
                            final String errorMessage = response.body();
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu. Mã lỗi: " + response.statusCode() + "\nChi tiết: " + errorMessage));
                            return;
                        }
                    } catch (Exception e) {
                        if (attempt == retries) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server sau " + retries + " lần thử: " + e.getMessage()));
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }
            }

            // Cập nhật giao diện sau khi lấy hết dữ liệu
            Platform.runLater(() -> {
                productList.setAll(allProducts);
                updateTableInfo();
                System.out.println("Tổng số sản phẩm đã tải: " + allProducts.size());
            });
        }).start();
    }

    private void loadCategories() {
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
                        .uri(URI.create("http://localhost:8081/api/admin/category/list?sort=id:desc"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Category API Response: " + response.body());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode data = root.path("data");
                    JsonNode items = data.path("items");
                    if (items.isArray()) {
                        List<Category> categories = objectMapper.convertValue(items, new TypeReference<List<Category>>(){});
                        System.out.println("Loaded categories: " + categories.size());
                        List<String> categoryNames = categories.stream()
                                .peek(category -> {
                                    categoryNameToId.put(category.getName(), category.getId());
                                    System.out.println("Mapping: " + category.getName() + " -> " + category.getId());
                                })
                                .map(Category::getName)
                                .collect(Collectors.toList());
                        Platform.runLater(() -> {
                            cmbCategory.getItems().clear();
                            categoryCombo.getItems().clear();
                            cmbCategory.getItems().add("Tất cả danh mục");
                            cmbCategory.getItems().addAll(categoryNames);
                            categoryCombo.getItems().addAll(categoryNames);
                            if (!categoryNames.isEmpty()) {
                                categoryCombo.setValue(categoryNames.get(0));
                                System.out.println("Default category set to: " + categoryNames.get(0));
                            } else {
                                System.out.println("No categories loaded!");
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu danh mục không phải là mảng."));
                    }
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    final String errorMessage = response.body();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh mục: Mã lỗi: " + response.statusCode() + "\nChi tiết: " + errorMessage));
                }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fxml/AddProductWindow.fxml"));
            Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setProductController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Thêm sản phẩm mới");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            controller.loadCategories();

            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở cửa sổ thêm sản phẩm: " + e.getMessage());
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

    @FXML
    private void handleUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageUrlField.setText("file:" + selectedImageFile.getAbsolutePath());
            loadProductImage("file:" + selectedImageFile.getAbsolutePath());
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Hiện tại API không hỗ trợ upload file. Vui lòng nhập URL ảnh vào ô này.");
        }
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

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            String firstImageUrl = product.getImageUrl().get(0);
            imageUrlField.setText(firstImageUrl);
            loadProductImage(firstImageUrl);
        } else {
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
        selectedImageFile = null;

        formTitle.setText("Thông tin sản phẩm");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);

        idLabel.setText("Tự sinh");
        nameField.clear();
        categoryCombo.setValue(categoryCombo.getItems().isEmpty() ? null : categoryCombo.getItems().get(0));
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

        String selectedCategoryName = categoryCombo.getValue();
        if (selectedCategoryName == null || selectedCategoryName.equals("Tất cả danh mục")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn một danh mục hợp lệ!");
            return null;
        }

        Integer categoryId = categoryNameToId.get(selectedCategoryName);
        if (categoryId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy ID cho danh mục: " + selectedCategoryName + ". Vui lòng tải lại danh mục!");
            return null;
        }

        data.put("categoryId", categoryId);
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

        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty() || categoryCombo.getValue().equals("Tất cả danh mục")) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn một danh mục hợp lệ!");
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

        return true;
    }

    // ================ API OPERATIONS ================
    public void createProduct(Map<String, Object> data) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8081/api/admin/products"))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getAccessToken())
                .timeout(Duration.ofSeconds(10));

        // Xây dựng body multipart
        MultipartBodyPublisher publisher = new MultipartBodyPublisher()
                .addPart("name", data.get("name").toString())
                .addPart("categoryId", String.valueOf(data.get("categoryId")))
                .addPart("price", data.get("price").toString())
                .addPart("stockQuantity", String.valueOf(data.get("stockQuantity")))
                .addPart("discount", data.get("discount").toString())
                .addPart("description", data.get("description").toString())
                .addPart("size", data.get("size").toString());

        // Thêm file nếu có
        if (data.get("imageUrl") != null && data.get("imageUrl") instanceof File) {
            File imageFile = (File) data.get("imageUrl");
            if (imageFile.exists()) {
                publisher.addPart("imageUrl", imageFile);
            } else {
                System.out.println("File imageUrl không tồn tại: " + imageFile.getAbsolutePath());
                throw new Exception("File hình ảnh không tồn tại.");
            }
        } else {
            publisher.addPart("imageUrl", (File) null);
        }

        HttpRequest request = requestBuilder
                .header("Content-Type", publisher.getContentType())
                .POST(publisher.build())
                .build();

        System.out.println("Gửi yêu cầu tới: " + request.uri());
        System.out.println("Headers: " + request.headers());

        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Phản hồi từ API - Status: " + response.statusCode());
        System.out.println("Phản hồi từ API - Body: " + response.body());

        // Kiểm tra mã trạng thái và body
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            // Phân tích body để kiểm tra thành công
            JsonNode root = getObjectMapper().readTree(response.body());
            String message = root.path("message").asText();
            if ("Create product success".equals(message)) {
                return; // Thành công, không ném ngoại lệ
            }
        }

        // Nếu không thành công, ném ngoại lệ
        throw new Exception("Không thể tạo sản phẩm: Mã lỗi " + response.statusCode() + " - " + response.body());
    }
    private void updateProduct(int id, Map<String, Object> data) {
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

                // In dữ liệu JSON để kiểm tra
                String jsonData = objectMapper.writeValueAsString(data);
                System.out.println("Dữ liệu gửi đi: " + jsonData);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/" + id))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .method("PUT", HttpRequest.BodyPublishers.ofString(jsonData))
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body();

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    String message = root.path("message").asText();
                    if ("Update product success".equals(message)) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật sản phẩm '" + data.get("name") + "' thành công!");
                            Product updatedProduct = objectMapper.convertValue(data, Product.class);
                            updatedProduct.setId(id);
                            updatedProduct.setCategoryName(categoryCombo.getValue());
                            int index = productList.indexOf(selectedProduct);
                            if (index >= 0) {
                                productList.set(index, updatedProduct);
                            }
                            productTable.refresh();
                            resetForm();
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật thất bại: " + message));
                    }
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    Platform.runLater(() -> {
                        String errorMsg = "Cập nhật thất bại. Mã lỗi: " + response.statusCode();
                        if (!responseBody.isEmpty()) {
                            errorMsg += "\nChi tiết: " + responseBody;
                        }
                        showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    // Hàm handleSave: Xử lý sự kiện nhấn nút Lưu/Cập nhật
    @FXML
    private void handleSave() throws Exception {
        if (!validateForm()) {
            return;
        }

        saveBtn.setDisable(true); // Vô hiệu hóa nút trong khi xử lý
        Map<String, Object> data = createProductData();
        if (data == null) {
            saveBtn.setDisable(false);
            return;
        }

        if (isEditMode && selectedProduct != null) {
            updateProduct(selectedProduct.getId(), data);
        } else {
            createProduct(data); // Giữ lại cho trường hợp thêm mới
        }
        saveBtn.setDisable(false); // Kích hoạt lại nút sau khi xử lý
    }

    private void deleteProduct(int id) {
        executeApiCall("DELETE", BASE_URL + "/" + id, null, "Đã xóa sản phẩm thành công!");
    }

    private void executeApiCall(String method, String urlStr, Map<String, Object> data, String successMessage) {
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

                HttpRequest.BodyPublisher bodyPublisher = data != null ? HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(data)) : HttpRequest.BodyPublishers.noBody();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .method(method, bodyPublisher)
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                final String responseBody = response.body();
                if (response.statusCode() == 200) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
                        loadProducts();
                        if ("DELETE".equals(method)) {
                            resetForm();
                        }
                    });
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    Platform.runLater(() -> {
                        String errorMsg = "Thao tác thất bại. Mã lỗi: " + response.statusCode();
                        if (!responseBody.isEmpty()) {
                            errorMsg += "\nChi tiết: " + responseBody;
                        }
                        showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg);
                    });
                }
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

    private void redirectToLogin() {
        Stage stage = (Stage) mainContent.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Login.fxml", "Đăng nhập");
        SessionManager.getInstance().clearSession();
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
            SessionManager.getInstance().clearSession();
            SceneManager.changeScene((Stage) mainContent.getScene().getWindow(), "/com/fxml/Login.fxml", "Đăng nhập");
        }
    }

    // ================ NEW WINDOW SUPPORT ================
    public Map<String, Integer> getCategoryNameToId() {
        return categoryNameToId;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}