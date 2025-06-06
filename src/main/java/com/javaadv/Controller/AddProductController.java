package com.javaadv.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.Model.Category;
import com.javaadv.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddProductController {

    @FXML private VBox addProductWindow;
    @FXML private ImageView previewImage;
    @FXML private TextField imageUrlFieldDialog;
    @FXML private TextField sizeFieldDialog;
    @FXML private TextField nameFieldDialog;
    @FXML private TextArea descAreaDialog;
    @FXML private TextField priceFieldDialog;
    @FXML private TextField discountFieldDialog;
    @FXML private TextField stockFieldDialog;
    @FXML private ComboBox<String> categoryComboDialog;
    @FXML private Button uploadButton;
    @FXML private Button saveButtonDialog;
    @FXML private Button cancelButtonDialog;

    private ProductController productController;
    private File selectedImageFileDialog;

    public void setProductController(ProductController productController) {
        this.productController = productController;
        setupFormValidation();
        loadCategories();
    }

    public void loadCategories() {
        new Thread(() -> {
            try {
                String token = SessionManager.getInstance().getAccessToken();
                if (token == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy token. Vui lòng đăng nhập lại."));
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/admin/category/list?sort=id:desc"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = productController.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Category API Response: " + response.body());

                if (response.statusCode() == 200) {
                    JsonNode root = productController.getObjectMapper().readTree(response.body());
                    JsonNode data = root.path("data");
                    JsonNode items = data.path("items");
                    if (items.isArray()) {
                        List<Category> categories = productController.getObjectMapper().convertValue(items, new TypeReference<List<Category>>(){});
                        System.out.println("Loaded categories: " + categories.size());
                        if (categories.isEmpty()) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không có danh mục nào được tải. Vui lòng kiểm tra API hoặc thêm danh mục trước."));
                            return;
                        }
                        List<String> categoryNames = categories.stream()
                                .peek(category -> productController.getCategoryNameToId().put(category.getName(), category.getId()))
                                .map(Category::getName)
                                .collect(Collectors.toList());
                        Platform.runLater(() -> {
                            categoryComboDialog.getItems().clear();
                            categoryComboDialog.getItems().addAll(categoryNames);
                            if (!categoryNames.isEmpty()) {
                                categoryComboDialog.setValue(categoryNames.get(0));
                                System.out.println("Default category set to: " + categoryNames.get(0));
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu danh mục không phải là mảng."));
                    }
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh mục: Mã lỗi: " + response.statusCode() + "\nChi tiết: " + response.body()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh mục: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void setupFormValidation() {
        nameFieldDialog.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.length() > 100) {
                nameFieldDialog.setText(oldText);
            }
        });

        priceFieldDialog.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*\\.?\\d*")) {
                priceFieldDialog.setText(oldText);
            }
        });

        stockFieldDialog.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                stockFieldDialog.setText(oldText);
            }
        });

        discountFieldDialog.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*\\.?\\d*")) {
                discountFieldDialog.setText(oldText);
            }
        });

        sizeFieldDialog.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                sizeFieldDialog.setText(oldText);
            }
        });
    }

    @FXML
    private void handleUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        selectedImageFileDialog = fileChooser.showOpenDialog(stage);

        if (selectedImageFileDialog != null) {
            imageUrlFieldDialog.setText("file:" + selectedImageFileDialog.getAbsolutePath());
            Image image = new Image(selectedImageFileDialog.toURI().toString(), 100, 100, true, true);
            previewImage.setImage(image);
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Hiện tại API không hỗ trợ upload file. Vui lòng nhập URL ảnh vào ô này sau khi lưu.");
        } else {
            imageUrlFieldDialog.setText("");
        }
    }

    @FXML
    private void handleSaveNewProduct() {
        if (!validateFormDialog()) return;

        Map<String, Object> data = createProductDataDialog();
        if (data != null) {
            try {
                System.out.println("Sending product data: " + data);
                productController.createProduct(data);
                // Hiển thị thông báo thành công (màu xanh)
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã được tạo thành công!");
                Stage stage = (Stage) saveButtonDialog.getScene().getWindow();
                stage.close();
            } catch (Exception e) {
                // Hiển thị thông báo lỗi (màu đỏ)
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo sản phẩm: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void handleCancelDialog() {
        Stage stage = (Stage) cancelButtonDialog.getScene().getWindow();
        stage.close();
    }

    private Map<String, Object> createProductDataDialog() {
        Map<String, Object> data = new HashMap<>();
        String name = nameFieldDialog.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên sản phẩm không được để trống!");
            return null;
        }

        String selectedCategoryName = categoryComboDialog.getValue();
        if (selectedCategoryName == null || selectedCategoryName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn một danh mục hợp lệ!");
            return null;
        }

        Integer categoryId = productController.getCategoryNameToId().get(selectedCategoryName);
        if (categoryId == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy ID cho danh mục: " + selectedCategoryName + ". Vui lòng tải lại danh mục!");
            return null;
        }

        if (!isCategoryValid(categoryId)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Danh mục với ID " + categoryId + " không tồn tại trong hệ thống!");
            return null;
        }

        data.put("name", name);
        data.put("categoryId", categoryId.longValue());

        try {
            data.put("price", new BigDecimal(priceFieldDialog.getText()));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm phải là số hợp lệ!");
            return null;
        }

        try {
            data.put("stockQuantity", Integer.parseInt(stockFieldDialog.getText()));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng tồn phải là số nguyên hợp lệ!");
            return null;
        }

        try {
            data.put("discount", new BigDecimal(discountFieldDialog.getText()));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giảm giá phải là số hợp lệ!");
            return null;
        }

        data.put("description", descAreaDialog.getText().trim());

        String sizeText = sizeFieldDialog.getText().trim();
        if (sizeText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Kích thước không được để trống!");
            return null;
        }
        data.put("size", sizeText);

        if (selectedImageFileDialog != null) {
            data.put("imageUrl", selectedImageFileDialog);
        } else {
            data.put("imageUrl", null);
        }

        System.out.println("Prepared product data: " + data);
        return data;
    }
    private boolean isCategoryValid(Integer categoryId) {
        try {
            String token = SessionManager.getInstance().getAccessToken();
            if (token == null) {
                System.out.println("No token found for category validation");
                return false;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/admin/category/" + categoryId))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = productController.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Category validation response for ID " + categoryId + ": Status=" + response.statusCode() + ", Body=" + response.body());
            if (response.statusCode() != 200) {
                System.out.println("Category ID " + categoryId + " is invalid or does not exist.");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error validating category ID " + categoryId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateFormDialog() {
        String name = nameFieldDialog.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên sản phẩm không được để trống!");
            nameFieldDialog.requestFocus();
            return false;
        }

        if (name.length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên sản phẩm phải có ít nhất 3 ký tự!");
            nameFieldDialog.requestFocus();
            return false;
        }

        if (categoryComboDialog.getValue() == null || categoryComboDialog.getValue().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn một danh mục hợp lệ!");
            categoryComboDialog.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceFieldDialog.getText());
            if (price < 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm không được âm!");
                priceFieldDialog.requestFocus();
                return false;
            }
            if (price > 999999999) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm quá lớn!");
                priceFieldDialog.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá sản phẩm phải là số hợp lệ!");
            priceFieldDialog.requestFocus();
            return false;
        }

        try {
            int stock = Integer.parseInt(stockFieldDialog.getText());
            if (stock < 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng tồn không được âm!");
                stockFieldDialog.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số lượng tồn phải là số nguyên hợp lệ!");
            stockFieldDialog.requestFocus();
            return false;
        }

        try {
            double discount = Double.parseDouble(discountFieldDialog.getText());
            if (discount < 0 || discount > 100) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giảm giá phải từ 0 đến 100%!");
                discountFieldDialog.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giảm giá phải là số hợp lệ!");
            discountFieldDialog.requestFocus();
            return false;
        }

        String sizeText = sizeFieldDialog.getText().trim();
        if (!sizeText.isEmpty()) {
            try {
                Integer.parseInt(sizeText);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Kích thước phải là số nguyên hợp lệ!");
                sizeFieldDialog.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            String bgColor = "#d4edda"; // Màu xanh cho thành công
            if (type == Alert.AlertType.ERROR) bgColor = "#f8d7da"; // Màu đỏ cho lỗi
            else if (type == Alert.AlertType.WARNING) bgColor = "#fff3cd"; // Màu vàng cho cảnh báo

            alert.getDialogPane().setStyle("-fx-background-color: " + bgColor + "; -fx-border-radius: 5;");
            alert.showAndWait();
        });
    }
}
