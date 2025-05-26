package com.javaadv.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javaadv.Model.Category;
import com.javaadv.Model.CategoryResponse;
import com.javaadv.SceneManager;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
    @FXML private Label createdAtLabel; // Thêm biến cho Label createdAt
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    // Data and Configuration
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final String BASE_URL = "http://localhost:8081/api/admin/category";
    private final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ4MDkzNDA0LCJleHAiOjE3NDk1MzM0MDR9.vXbj0tlK7jhbs_EToWO2AK1DSl4oQK6fGe_cinsZqEY";

    private Category selectedCategory;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, java.util.ResourceBundle resourceBundle) {
        setupTable();
        setupSearch();
        setupForm();
        loadCategories();
    }

    // ================ SETUP METHODS ================
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Định dạng hiển thị cho LocalDateTime trong TableView
        colCreatedAt.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
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

    // ================ DATA LOADING ================
    private void loadCategories() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/list?sort=id:desc");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);

                System.out.println("Sending request to: " + url);
                System.out.println("Headers: Authorization=Bearer [token], Content-Type=application/json");

                int responseCode = conn.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    // Deserialize into CategoryResponse
                    CategoryResponse categoryResponse = objectMapper.readValue(response.toString(), CategoryResponse.class);
                    List<Category> categories = categoryResponse.getData().getItems();
                    System.out.println("Response Body: " + response.toString());
                    Platform.runLater(() -> {
                        categoryList.setAll(categories);
                        filterCategories();
                    });
                } else {
                    handleApiError(responseCode, conn);
                }
                conn.disconnect();
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    // ================ FILTERING ================
    private void filterCategories() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        ObservableList<Category> filtered = categoryList.filtered(category ->
                searchText.isEmpty() ||
                        category.getName().toLowerCase().contains(searchText) ||
                        String.valueOf(category.getId()).contains(searchText));
        categoryTable.setItems(filtered);
    }

    // ================ EVENT HANDLERS ================
    @FXML
    private void handleAdd() {
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
        if (!validateForm()) return;

        Map<String, Object> data = createCategoryData();
        if (isEditMode && selectedCategory != null) {
            updateCategory(selectedCategory.getId(), data);
        } else {
            createCategory(data);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory != null && isEditMode) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Bạn có chắc chắn muốn xóa danh mục '" + selectedCategory.getName() + "'?\nThao tác này không thể hoàn tác!");
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                deleteCategory(selectedCategory.getId());
            }
        }
    }

    @FXML
    private void handleClear() {
        resetForm();
        categoryTable.getSelectionModel().clearSelection();
    }

    // ================ FORM OPERATIONS ================
    private void loadCategoryToForm(Category category) {
        selectedCategory = category;
        isEditMode = true;

        formTitle.setText("Chi tiết danh mục - " + category.getName());
        saveBtn.setText("Cập nhật");
        deleteBtn.setVisible(true);

        idLabel.setText(String.valueOf(category.getId()));
        nameField.setText(category.getName());

        // Hiển thị createdAt trên createdAtLabel
        if (category.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            createdAtLabel.setText(formatter.format(category.getCreatedAt()));
        } else {
            createdAtLabel.setText("Chưa có dữ liệu");
        }
    }

    private void resetForm() {
        selectedCategory = null;
        isEditMode = false;

        formTitle.setText("Thông tin danh mục");
        saveBtn.setText("Thêm mới");
        deleteBtn.setVisible(false);

        idLabel.setText("Tự sinh");
        nameField.clear();
        createdAtLabel.setText("Chưa cập nhật"); // Đặt lại giá trị mặc định
    }

    private Map<String, Object> createCategoryData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", nameField.getText().trim());
        data.put("createdAt", LocalDateTime.now().toString());
        return data;
    }

    // ================ VALIDATION ================
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty() || nameField.getText().trim().length() < 3) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên danh mục phải có ít nhất 3 ký tự!");
            nameField.requestFocus();
            return false;
        }
        return true;
    }

    // ================ API OPERATIONS ================
    private void createCategory(Map<String, Object> data) {
        executeApiCall("POST", BASE_URL, data, "Đã thêm danh mục '" + data.get("name") + "' thành công!");
    }

    private void updateCategory(int id, Map<String, Object> data) {
        executeApiCall("PUT", BASE_URL + "/" + id, data, "Đã cập nhật danh mục '" + data.get("name") + "' thành công!");
    }

    private void deleteCategory(int id) {
        executeApiCall("DELETE", BASE_URL + "/" + id, null, "Đã xóa danh mục thành công!");
    }

    private void executeApiCall(String method, String urlStr, Map<String, Object> data, String successMessage) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);

                System.out.println("Sending " + method + " request to: " + urlStr);
                System.out.println("Headers: Authorization=Bearer [token], Content-Type=application/json");

                if (data != null) {
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = objectMapper.writeValueAsBytes(data);
                        os.write(input);
                        os.flush();
                        System.out.println("Request Body: " + new String(input));
                    }
                }

                int responseCode = conn.getResponseCode();
                System.out.println("Response Code: " + responseCode);

                String responseMessage = readResponse(conn, responseCode);
                System.out.println("Response Body: " + responseMessage);

                conn.disconnect();

                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
                        loadCategories();
                        if ("DELETE".equals(method)) resetForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Thao tác thất bại. Mã lỗi: " + responseCode + "\nChi tiết: " + responseMessage);
                    }
                });
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server: " + e.getMessage()));
            }
        }).start();
    }

    private String readResponse(HttpURLConnection conn, int responseCode) throws IOException {
        StringBuilder response = new StringBuilder();
        BufferedReader reader = responseCode >= 400
                ? new BufferedReader(new InputStreamReader(conn.getErrorStream()))
                : new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        return response.toString();
    }

    private void handleApiError(int responseCode, HttpURLConnection conn) throws IOException {
        String responseMessage = readResponse(conn, responseCode);
        System.out.println("API Error - Response Code: " + responseCode + ", Body: " + responseMessage);
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu. Mã lỗi: " + responseCode + "\nChi tiết: " + responseMessage));
    }

    // ================ UTILITY METHODS ================
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

    // ================ NAVIGATION HANDLERS ================
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