package com.javaadv.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javaadv.Model.Category;
import com.javaadv.Model.CategoryResponse;
import com.javaadv.SceneManager;
import com.javaadv.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryController implements Initializable {

    // FXML Components - Navigation
    @FXML private Button btnOverview;
    @FXML private Button btnProductManagement;
    @FXML private Button btnUserManagement;
    @FXML private Button btnOrderManagement;

    // FXML Components - Table and Controls
    @FXML private TextField txtSearch;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, LocalDateTime> colCreatedAt;

    // FXML Components - Detail Form
    @FXML private Label formTitle;
    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private Label createdAtLabel;
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    // Data and Configuration
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private final String BASE_URL = "http://localhost:8081/api/admin/category";
    private Category selectedCategory;
    private boolean isEditMode = false;
    private boolean isProcessing = false;

    @Override
    public void initialize(URL url, java.util.ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupForm();
        loadCategories();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colCreatedAt.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(formatter.format(item));
            }
        });

        categoryTable.setItems(categoryList);
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        categoryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 || event.getClickCount() == 2) {
                Category category = categoryTable.getSelectionModel().getSelectedItem();
                if (category != null) loadCategoryToForm(category);
            }
        });
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldText, newText) -> filterCategories());
    }

    private void setupForm() {
        resetForm();
        setupFormValidation();
    }

    private void setupFormValidation() {
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.length() > 100) nameField.setText(oldText);
        });
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
                        .uri(URI.create(BASE_URL + "/list?sort=id:desc"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .GET()
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    CategoryResponse categoryResponse = objectMapper.readValue(response.body(), CategoryResponse.class);
                    List<Category> categories = categoryResponse.getData().getItems();
                    Platform.runLater(() -> categoryList.setAll(categories));
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    });
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu (Mã lỗi: " + response.statusCode() + ")"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    private void filterCategories() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        ObservableList<Category> filtered = categoryList.filtered(category ->
                searchText.isEmpty() ||
                        category.getName().toLowerCase().contains(searchText) ||
                        String.valueOf(category.getId()).contains(searchText));
        categoryTable.setItems(filtered);
    }

    @FXML
    private void handleAdd() {
        if (isProcessing) return;
        resetForm();
        isEditMode = false;
        selectedCategory = null;
        formTitle.setText("Thêm danh mục mới");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);
        nameField.requestFocus();
    }

    @FXML
    private void handleSave() {
        if (isProcessing) return;
        if (!validateForm()) return;

        setFormProcessing(true);
        Map<String, Object> data = new HashMap<>();
        data.put("name", nameField.getText().trim());
        if (isEditMode && selectedCategory != null) {
            updateCategory(selectedCategory.getId(), data);
        } else {
            createCategory(data);
        }
    }

    @FXML
    private void handleDelete() {
        if (isProcessing || selectedCategory == null || !isEditMode) return;
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa danh mục '" + selectedCategory.getName() + "'?");
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            setFormProcessing(true);
            deleteCategory(selectedCategory.getId());
        }
    }

    @FXML
    private void handleClear() {
        if (isProcessing) return;
        resetForm();
        categoryTable.getSelectionModel().clearSelection();
    }

    private void loadCategoryToForm(Category category) {
        selectedCategory = category;
        isEditMode = true;
        formTitle.setText("Chi tiết danh mục - " + category.getName());
        saveBtn.setText("Cập nhật");
        deleteBtn.setVisible(true);
        idLabel.setText(String.valueOf(category.getId()));
        nameField.setText(category.getName());
        createdAtLabel.setText(category.getCreatedAt() != null ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(category.getCreatedAt()) : "Chưa có dữ liệu");
    }

    private void resetForm() {
        selectedCategory = null;
        isEditMode = false;
        formTitle.setText("Thông tin danh mục");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);
        idLabel.setText("Tự sinh");
        nameField.clear();
        createdAtLabel.setText("Chưa cập nhật");
    }

    private boolean validateForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên danh mục không được để trống!");
            nameField.requestFocus();
            return false;
        }
        if (name.length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên danh mục phải có ít nhất 3 ký tự!");
            nameField.requestFocus();
            return false;
        }
        if (name.length() > 100) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên danh mục không được vượt quá 100 ký tự!");
            nameField.requestFocus();
            return false;
        }
        return true;
    }

    private void createCategory(Map<String, Object> data) {
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

                String jsonBody = objectMapper.writeValueAsString(data);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    if (response.statusCode() == 201) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm danh mục '" + data.get("name") + "' thành công!");
                        loadCategories();
                        resetForm();
                    } else if (response.statusCode() == 401) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    } else if (response.statusCode() == 409) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Danh mục đã tồn tại! Vui lòng chọn tên khác.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại (Mã lỗi: " + response.statusCode() + ")");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void updateCategory(int id, Map<String, Object> data) {
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

                String jsonBody = objectMapper.writeValueAsString(data);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/" + id))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    if (response.statusCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật danh mục '" + data.get("name") + "' thành công!");
                        loadCategories();
                    } else if (response.statusCode() == 401) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại (Mã lỗi: " + response.statusCode() + ")");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void deleteCategory(int id) {
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
                        .uri(URI.create(BASE_URL + "/" + id))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .DELETE()
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    if (response.statusCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa danh mục thành công!");
                        loadCategories();
                        resetForm();
                    } else if (response.statusCode() == 401) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
                        redirectToLogin();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại (Mã lỗi: " + response.statusCode() + ")");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setFormProcessing(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void redirectToLogin() {
        Stage stage = (Stage) categoryTable.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Login.fxml", "Đăng nhập");
        SessionManager.getInstance().clearSession();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setFormProcessing(boolean processing) {
        isProcessing = processing;
        nameField.setDisable(processing);
        saveBtn.setDisable(processing);
        deleteBtn.setDisable(processing);
        clearBtn.setDisable(processing);
        categoryTable.setDisable(processing);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
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

    @FXML
    public void handleOverview(ActionEvent event) {
        SceneManager.changeScene((Stage) btnOverview.getScene().getWindow(), "/com/fxml/Dashboard.fxml", "Quản lý tổng quan");
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) btnUserManagement.getScene().getWindow(), "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleProductManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) btnProductManagement.getScene().getWindow(), "/com/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
    }

    @FXML
    public void handleOrderManagement(ActionEvent event) {
        SceneManager.changeScene((Stage) btnOrderManagement.getScene().getWindow(), "/com/fxml/OrderManagement.fxml", "Quản lý đơn hàng");
    }
}