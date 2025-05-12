package com.javaadv;

import com.javaadv.Model.Order;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class OrderController {

    @FXML
    private Button btnOverview;
    @FXML
    private Button btnUserManagement;
    @FXML
    private Button btnCategoryManagement;


    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> colCompany;
    @FXML private TableColumn<Order, String> colAirport;
    @FXML private TableColumn<Order, String> colDeliveryDate;
    @FXML private TableColumn<Order, String> colAmount;
    @FXML private TableColumn<Order, String> colStatus;

    @FXML private TextField txtCompany;
    @FXML private TextField txtAirport;
    @FXML private TextField txtDeliveryDate;
    @FXML private TextField txtAmount;
    @FXML private TextField txtStatus;
    @FXML private TextField txtSearch;

    private ObservableList<Order> orderList = FXCollections.observableArrayList();

    // Dữ liệu mẫu về giày
    private final String[] SHOE_COMPANIES = {
            "Nike", "Adidas", "Puma", "Bitis", "Converse",
            "Vans", "New Balance", "Reebok", "Asics", "Skechers"
    };

    private final String[] SHOE_TYPES = {
            "Air Force 1", "Ultraboost", "Classic Leather", "Chuck Taylor", "Old Skool",
            "574", "Club C", "Gel-Kayano", "Go Walk", "Superstar"
    };

    private final String[] DELIVERY_STATUSES = {
            "Đang xử lý", "Đã đóng gói", "Đang vận chuyển", "Đã giao hàng", "Đã hủy"
    };

    @FXML
    public void initialize() {
        // Tạo dữ liệu fake
        generateFakeData();

        // Liên kết cột với thuộc tính
        colCompany.setCellValueFactory(new PropertyValueFactory<>("company"));
        colAirport.setCellValueFactory(new PropertyValueFactory<>("airport"));
        colDeliveryDate.setCellValueFactory(new PropertyValueFactory<>("deliveryDate"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Thiết lập tìm kiếm
        setupSearchFunctionality();

        // Hiển thị dữ liệu
        orderTable.setItems(orderList);

        // Bắt sự kiện chọn hàng trong bảng
        orderTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    showOrderDetails(newValue);
                });
    }

    private void generateFakeData() {
        for (int i = 1; i <= 100; i++) {
            String company = SHOE_COMPANIES[i % SHOE_COMPANIES.length];
            String shoeType = SHOE_TYPES[i % SHOE_TYPES.length];
            String deliveryDate = String.format("%02d/%02d/2023",
                    (i % 28) + 1,
                    (i % 12) + 1);
            String amount = String.format("%,d VND", 500000 + (i * 100000));
            String status = DELIVERY_STATUSES[i % DELIVERY_STATUSES.length];

            // Sử dụng shoeType thay cho airport để phù hợp với ngữ cảnh giày
            orderList.add(new Order(company, shoeType, deliveryDate, amount, status));
        }
    }

    private void setupSearchFunctionality() {
        FilteredList<Order> filteredData = new FilteredList<>(orderList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(order -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (order.getCompany().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (order.getAirport().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Thực tế đây là loại giày
                } else if (order.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        SortedList<Order> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedData);
    }

    private void showOrderDetails(Order order) {
        txtCompany.setText(order.getCompany());
        txtAirport.setText(order.getAirport());
        txtDeliveryDate.setText(order.getDeliveryDate());
        txtAmount.setText(order.getAmount());
        txtStatus.setText(order.getStatus());
    }

    @FXML
    private void handleAdd() {
        if (validateFields()) {
            Order order = new Order(
                    txtCompany.getText(),
                    txtAirport.getText(),
                    txtDeliveryDate.getText(),
                    txtAmount.getText(),
                    txtStatus.getText()
            );

            orderList.add(order);
            clearFields();
        } else {
            showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin");
        }
    }

    @FXML
    private void handleUpdate() {
        Order selectedOrder = orderTable.getSelectionModel().getSelectedItem();
        if (selectedOrder != null) {
            if (validateFields()) {
                selectedOrder.setCompany(txtCompany.getText());
                selectedOrder.setAirport(txtAirport.getText());
                selectedOrder.setDeliveryDate(txtDeliveryDate.getText());
                selectedOrder.setAmount(txtAmount.getText());
                selectedOrder.setStatus(txtStatus.getText());

                orderTable.refresh();
                clearFields();
            } else {
                showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin");
            }
        } else {
            showAlert("Cảnh báo", "Vui lòng chọn đơn hàng cần sửa");
        }
    }



    @FXML
    private void handleDelete() {
        int selectedIndex = orderTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            orderTable.getItems().remove(selectedIndex);
            clearFields();
        } else {
            showAlert("Cảnh báo", "Vui lòng chọn đơn hàng cần xóa");
        }
    }

    @FXML
    private void handleClear() {
        clearFields();
        orderTable.getSelectionModel().clearSelection();
    }

    private boolean validateFields() {
        return !txtCompany.getText().isEmpty() &&
                !txtAirport.getText().isEmpty() &&
                !txtDeliveryDate.getText().isEmpty() &&
                !txtAmount.getText().isEmpty() &&
                !txtStatus.getText().isEmpty();
    }

    private void clearFields() {
        txtCompany.clear();
        txtAirport.clear();
        txtDeliveryDate.clear();
        txtAmount.clear();
        txtStatus.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleOverview(ActionEvent event) {
        Stage stage = (Stage) btnOverview.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/Dashboard.fxml", "Quản lý người dùng");
    }

    @FXML
    public void handleUserManagement(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/UserManagement.fxml", "Quản lý người dùng");
    }
    @FXML
    public void handleProductManagement(ActionEvent event) {
        Stage stage = (Stage) btnUserManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý Sản phẩm");
    }

    @FXML
    public void handleCategoryManagement(ActionEvent event) {
        Stage stage = (Stage) btnCategoryManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/CategoryManagement.fxml", "Quản lý Sản phẩm");
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), stage.getScene().getRoot());
        fadeOut.setFromValue(1.0);  // Mờ dần từ 100%
        fadeOut.setToValue(0.0);    // Đến 0% (biến mất)
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
