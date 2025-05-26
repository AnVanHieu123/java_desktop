package com.javaadv.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaadv.Model.ApiResponse;
import com.javaadv.Model.User;
import com.javaadv.SceneManager;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserController implements Initializable {

    @FXML private Button btnUserManagement;
    @FXML private Button btnOrderManagement;
    @FXML private Button btnCategoryManagement;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colAddress;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, String> colUpdatedAt;
    @FXML private TextField txtId;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtStatus;
    @FXML private TextField txtAddress;
    @FXML private TextField txtRole;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private TextField txtSearch;
    @FXML private Button btnStatistics;

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser = null;
    private static final String API_BASE_URL = "http://localhost:8081"; // Đúng với Swagger UI
    private String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHVhdmFuYW4yMDRAZ21haWwuY29tIiwiaWF0IjoxNzQ2Nzk1OTI4LCJleHAiOjE3NDgyMzU5Mjh9.lG-2UxVRmiFqK6jfg16HyIhFcT3fq2TYLJPLyOVJ7OI"; // Gán token từ Postman vào đây
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupTableSelectionListener();
        setupButtonActions();
        loadUsersFromAPI();
        setupSearchListener();
        btnStatistics.setOnAction(e -> showStatistics());
        userTable.setOnMouseClicked(this::handleTableClick);
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });
    }

    private void filterUsers(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        String lowerCaseKeyword = keyword.toLowerCase();

        ObservableList<User> filteredList = userList.filtered(user ->
                (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerCaseKeyword)) ||
                        String.valueOf(user.getId()).contains(lowerCaseKeyword) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseKeyword))
        );

        userTable.setItems(filteredList);
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        userTable.setItems(userList);

        userTable.setOnMouseClicked(this::handleTableRowClick);
    }

    private void handleTableRowClick(MouseEvent event) {
        User clickedUser = userTable.getSelectionModel().getSelectedItem();
        if (clickedUser != null) {
            selectedUser = clickedUser;
            populateFields(selectedUser);
        }
    }

    private void loadUsersFromAPI() {
        new Thread(() -> {
            int retries = 3;
            int timeout = 5000; // 5 giây
            for (int attempt = 1; attempt <= retries; attempt++) {
                try {
                    System.out.println("Thử lần " + attempt + " - Gọi API tại: " + API_BASE_URL + "/api/admin/users/list?pageNo=1&pageSize=10&sorts=fullName:desc");
                    System.out.println("Token: " + TOKEN);

                    URL url = new URL(API_BASE_URL + "/api/admin/users/list?pageNo=1&pageSize=10&sorts=fullName:desc");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(timeout);
                    conn.setReadTimeout(timeout);

                    int responseCode = conn.getResponseCode();
                    System.out.println("Mã phản hồi: " + responseCode);

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        System.out.println("Phản hồi API: " + response.toString());

                        // Deserialize thành ApiResponse
                        ApiResponse apiResponse = objectMapper.readValue(response.toString(), ApiResponse.class);
                        List<User> users = apiResponse.getData().getItems();

                        Platform.runLater(() -> {
                            userList.clear();
                            userList.addAll(users);
                            userTable.refresh();
                            // Thêm log để kiểm tra dữ liệu trong userList
                            System.out.println("Số lượng người dùng trong userList: " + userList.size());
                            userList.forEach(user -> System.out.println("User: " + user.getId() + ", FullName: " + user.getFullName() + ", Email: " + user.getEmail()));
                        });
                        break; // Thoát vòng lặp nếu thành công
                    } else {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();
                        System.out.println("Phản hồi lỗi: " + errorResponse.toString());

                        Map<String, Object> errorMap = objectMapper.readValue(errorResponse.toString(), Map.class);
                        String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu: " + message + " (Mã lỗi: " + responseCode + ")"));
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (attempt == retries) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối đến server sau " + retries + " lần thử: " + e.getMessage()));
                    }
                    try {
                        Thread.sleep(2000); // Chờ 2 giây trước khi thử lại
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @FXML
    private void showStatistics() {
        int totalUsers = userList.size();
        Map<String, Long> statusCount = userList.stream()
                .collect(Collectors.groupingBy(User::getStatus, Collectors.counting()));
        Map<String, Long> roleCount = userList.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

        StringBuilder content = new StringBuilder()
                .append("Tổng số người dùng: ").append(totalUsers).append("\n")
                .append("Phân bố theo trạng thái:\n");
        statusCount.forEach((status, count) -> content.append("- ").append(status).append(": ").append(count).append("\n"));
        content.append("Phân bố theo vai trò:\n");
        roleCount.forEach((role, count) -> content.append("- ").append(role).append(": ").append(count).append("\n"));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thống kê người dùng");
        alert.setHeaderText("Thông tin tổng hợp:");
        alert.setContentText(content.toString());
        alert.showAndWait();

        showChart(statusCount, roleCount);
    }

    private void showChart(Map<String, Long> statusCount, Map<String, Long> roleCount) {
        if (userList.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Thông báo", "Không có dữ liệu người dùng để hiển thị");
            return;
        }

        ObservableList<PieChart.Data> statusData = FXCollections.observableArrayList();
        statusCount.forEach((status, count) -> statusData.add(new PieChart.Data(status + " (" + count + ")", count)));

        ObservableList<PieChart.Data> roleData = FXCollections.observableArrayList();
        roleCount.forEach((role, count) -> roleData.add(new PieChart.Data(role + " (" + count + ")", count)));

        PieChart statusChart = new PieChart(statusData);
        statusChart.setTitle("Phân bố theo trạng thái");

        PieChart roleChart = new PieChart(roleData);
        roleChart.setTitle("Phân bố theo vai trò");

        String[] colors = {"#2ecc71", "#3498db", "#f39c12", "#e74c3c"};
        applyChartColors(statusChart, statusData, colors);
        applyChartColors(roleChart, roleData, colors);

        HBox chartsBox = new HBox(20, statusChart, roleChart);
        chartsBox.setPadding(new Insets(15));
        chartsBox.setAlignment(Pos.CENTER);
        VBox root = new VBox(10);
        root.getChildren().addAll(
                new Label("THỐNG KÊ NGƯỜI DÙNG") {{
                    setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
                }},
                chartsBox
        );
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa;");

        Stage stage = new Stage();
        stage.setScene(new Scene(root, 900, 500));
        stage.setTitle("Thống kê người dùng");
        stage.show();
    }

    private void applyChartColors(PieChart chart, ObservableList<PieChart.Data> data, String[] colors) {
        for (int i = 0; i < data.size(); i++) {
            data.get(i).getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
        }
        chart.setStyle("-fx-font-size: 12px;");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
    }

    private void setupTableSelectionListener() {
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedUser = newValue;
            if (selectedUser != null) {
                populateFields(selectedUser);
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
@FXML
    private void handleAdd(ActionEvent event) {
        try {
            User newUser = new User(
                    Integer.parseInt(txtId.getText().trim()),
                    txtFullName.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPhone.getText().trim(),
                    txtStatus.getText().trim(),
                    txtAddress.getText().trim(),
                    txtRole.getText().trim(),
                    "2025-05-24 08:25:00", // Thời gian hiện tại
                    "2025-05-24 08:25:00"
            );

            addUserToAPI(newUser);
            clearFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thêm người dùng thất bại: " + e.getMessage());
        }
    }
@FXML
    private void addUserToAPI(User user) {
        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(user);
                URL url = new URL(API_BASE_URL + "/api/admin/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    User addedUser = objectMapper.readValue(response.toString(), User.class);
                    Platform.runLater(() -> {
                        userList.add(addedUser);
                        userTable.refresh();
                        showAlert(Alert.AlertType.ERROR, "Thành công", "Thêm người dùng thành công!");
                    });
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Map<String, Object> errorMap = objectMapper.readValue(errorResponse.toString(), Map.class);
                    String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Thêm người dùng thất bại: " + message + " (Mã lỗi: " + responseCode + ")"));
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi gọi API: " + e.getMessage()));
            }
        }).start();
    }
@FXML
    private void handleEdit(ActionEvent event) {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn người dùng để sửa!");
            return;
        }

        try {
            selectedUser.setFullName(txtFullName.getText().trim());
            selectedUser.setEmail(txtEmail.getText().trim());
            selectedUser.setPhone(txtPhone.getText().trim());
            selectedUser.setStatus(txtStatus.getText().trim());
            selectedUser.setAddress(txtAddress.getText().trim());
            selectedUser.setRole(txtRole.getText().trim());
            selectedUser.setUpdatedAt("2025-05-24 08:25:00");

            updateUserInAPI(selectedUser);
            clearFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật người dùng thất bại: " + e.getMessage());
        }
    }

    private void updateUserInAPI(User user) {
        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(user);
                URL url = new URL(API_BASE_URL + "/api/admin/users/" + user.getId());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    User updatedUser = objectMapper.readValue(response.toString(), User.class);
                    Platform.runLater(() -> {
                        int index = userList.indexOf(selectedUser);
                        userList.set(index, updatedUser);
                        userTable.refresh();
                        showAlert(Alert.AlertType.ERROR, "Thành công", "Cập nhật người dùng thành công!");
                    });
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Map<String, Object> errorMap = objectMapper.readValue(errorResponse.toString(), Map.class);
                    String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật người dùng thất bại: " + message + " (Mã lỗi: " + responseCode + ")"));
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi gọi API: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn người dùng cần xóa");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn xóa người dùng '" + selectedUser.getFullName() + "'?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUserFromAPI(selectedUser.getId());
            clearFields();
        }
    }

    private void deleteUserFromAPI(int userId) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE_URL + "/api/admin/users/delete/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + TOKEN);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Platform.runLater(() -> {
                        userList.removeIf(user -> user.getId() == userId);
                        userTable.refresh();
                        showAlert(Alert.AlertType.ERROR, "Thành công", "Đã xóa người dùng thành công");
                    });
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Map<String, Object> errorMap = objectMapper.readValue(errorResponse.toString(), Map.class);
                    String message = errorMap.getOrDefault("message", "Không có thông báo lỗi").toString();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Xóa người dùng thất bại: " + message + " (Mã lỗi: " + responseCode + ")"));
                }
                conn.disconnect();
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi gọi API: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            if (selectedUser != null) {
                String detailText = "ID: " + selectedUser.getId() +
                        "\nHọ và tên: " + (selectedUser.getFullName() != null ? selectedUser.getFullName() : "Không có") +
                        "\nEmail: " + (selectedUser.getEmail() != null ? selectedUser.getEmail() : "Không có") +
                        "\nSố điện thoại: " + (selectedUser.getPhone() != null ? selectedUser.getPhone() : "Không có") +
                        "\nTrạng thái: " + (selectedUser.getStatus() != null ? selectedUser.getStatus() : "Không có") +
                        "\nĐịa chỉ: " + (selectedUser.getAddress() != null ? selectedUser.getAddress() : "Không có") +
                        "\nVai trò: " + (selectedUser.getRole() != null ? selectedUser.getRole() : "Không có") +
                        "\nNgày tạo: " + (selectedUser.getCreatedAt() != null ? selectedUser.getCreatedAt() : "Không có") +
                        "\nNgày cập nhật: " + (selectedUser.getUpdatedAt() != null ? selectedUser.getUpdatedAt() : "Không có");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Chi tiết người dùng");
                alert.setHeaderText(selectedUser.getFullName());
                alert.setContentText(detailText);
                alert.showAndWait();
            }
        } else if (event.getClickCount() == 1) {
            User clickedUser = userTable.getSelectionModel().getSelectedItem();
            if (clickedUser != null) {
                selectedUser = clickedUser;
                populateFields(selectedUser);
            }
        }
    }

    private void populateFields(User user) {
        if (user == null) return;

        Platform.runLater(() -> {
            txtId.setText(String.valueOf(user.getId()));
            txtFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
            txtStatus.setText(user.getStatus() != null ? user.getStatus() : "");
            txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");
            txtRole.setText(user.getRole() != null ? user.getRole() : "");

            txtFullName.requestFocus();
        });
    }

    @FXML
    private void clearFields() {
        txtId.setText("");
        txtFullName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtStatus.setText("");
        txtAddress.setText("");
        txtRole.setText("");
        selectedUser = null;
    }

    private void showAlert(Alert.AlertType error, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
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
        // Đã đang ở màn hình quản lý người dùng, không cần làm gì
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