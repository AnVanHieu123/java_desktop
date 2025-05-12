package com.javaadv;

import com.javaadv.Model.User;
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

import java.io.IOException;
import java.net.URL;
import java.util.*;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupTableSelectionListener(); // Đổi tên từ setupFocusListener để rõ ràng hơn
        setupButtonActions();
        loadFakeUsers(); // Load dữ liệu giả lập
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

        // Thêm sự kiện click một lần vào hàng để chọn và hiển thị thông tin
        userTable.setOnMouseClicked(this::handleTableRowClick);
    }

    private void handleTableRowClick(MouseEvent event) {
        User clickedUser = userTable.getSelectionModel().getSelectedItem();
        if (clickedUser != null) {
            selectedUser = clickedUser;
            populateFields(selectedUser);
        }
    }

    private void loadFakeUsers() {
        List<User> fakeUsers = new ArrayList<>();
        String[] lastNames = {"Nguyen", "Tran", "Le", "Pham", "Hoang"};
        String[] middleNames = {"Van", "Thi", "Minh", "Ngoc", "Anh"};
        String[] firstNames = {"An", "Binh", "Cuong", "Dung", "Hanh"};
        String[] statuses = {"Active", "Inactive"};
        String[] roles = {"Admin", "User"};
        Random random = new Random();

        for (int i = 1; i <= 100; i++) {
            String fullName = lastNames[random.nextInt(lastNames.length)] + " " +
                    middleNames[random.nextInt(middleNames.length)] + " " +
                    firstNames[random.nextInt(firstNames.length)];
            String email = "user" + i + "@example.com";
            String phone = "09" + String.format("%07d", random.nextInt(10000000));
            String status = statuses[random.nextInt(statuses.length)];
            String address = String.format("%d Đường ABC, Quận %d, TP HCM", i, random.nextInt(12) + 1);
            String role = roles[random.nextInt(roles.length)];
            String createdAt = "2025-05-01 10:00:00";
            String updatedAt = "2025-05-02 12:00:00";

            User user = new User(i, fullName, email, phone, status, address, role, createdAt, updatedAt);
            fakeUsers.add(user);
        }

        userList.setAll(fakeUsers);
        userTable.refresh();
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
            showAlert("Thông báo", "Không có dữ liệu người dùng để hiển thị");
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

    private void handleAdd(ActionEvent event) {
        try {
            int newId = userList.stream()
                    .mapToInt(User::getId)
                    .max()
                    .orElse(0) + 1;

            User newUser = new User(
                    newId,
                    txtFullName.getText().trim(),
                    txtEmail.getText().trim(),
                    txtPhone.getText().trim(),
                    txtStatus.getText().trim(),
                    txtAddress.getText().trim(),
                    txtRole.getText().trim(),
                    "2025-05-01 10:00:00", // Giả lập createdAt
                    "2025-05-01 10:00:00"  // Giả lập updatedAt
            );

            userList.add(newUser);
            userTable.refresh();
            showAlert("Thành công", "Thêm người dùng thành công!");
            clearFields();
        } catch (Exception e) {
            showAlert("Lỗi", "Thêm người dùng thất bại: " + e.getMessage());
        }
    }

    private void handleEdit(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Lỗi", "Vui lòng chọn người dùng để sửa!");
            return;
        }

        try {
            selectedUser.setFullName(txtFullName.getText().trim());
            selectedUser.setEmail(txtEmail.getText().trim());
            selectedUser.setPhone(txtPhone.getText().trim());
            selectedUser.setStatus(txtStatus.getText().trim());
            selectedUser.setAddress(txtAddress.getText().trim());
            selectedUser.setRole(txtRole.getText().trim());
            selectedUser.setUpdatedAt("2025-05-02 12:00:00"); // Cập nhật thời gian giả lập

            userTable.refresh();
            showAlert("Thành công", "Cập nhật người dùng thành công!");
            clearFields();
        } catch (Exception e) {
            showAlert("Lỗi", "Cập nhật người dùng thất bại: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Lỗi", "Vui lòng chọn người dùng cần xóa");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Bạn có chắc chắn muốn xóa người dùng '" + selectedUser.getFullName() + "'?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userList.remove(selectedUser);
            userTable.refresh();
            clearFields();
            showAlert("Thành công", "Đã xóa người dùng thành công");
        }
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
            // Đã được xử lý trong handleTableRowClick
            User clickedUser = userTable.getSelectionModel().getSelectedItem();
            if (clickedUser != null) {
                selectedUser = clickedUser;
                populateFields(selectedUser);
            }
        }
    }

    private void populateFields(User user) {
        if (user == null) return;

        // Sử dụng Platform.runLater để đảm bảo các thay đổi UI được thực hiện trên JavaFX Application Thread
        Platform.runLater(() -> {
            txtId.setText(String.valueOf(user.getId()));
            txtFullName.setText(user.getFullName() != null ? user.getFullName() : "");
            txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
            txtStatus.setText(user.getStatus() != null ? user.getStatus() : "");
            txtAddress.setText(user.getAddress() != null ? user.getAddress() : "");
            txtRole.setText(user.getRole() != null ? user.getRole() : "");

            // Đặt trọng tâm vào trường họ tên sau khi điền thông tin
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