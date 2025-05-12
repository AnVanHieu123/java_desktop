package com.javaadv;


import com.javaadv.Model.Category;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class CategoryController {
    @FXML private Button btnOrderManagement;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colType;
    @FXML private TableColumn<Category, Integer> colShoeCount;
    @FXML private Button btnOverview;
    @FXML private Button btnProductManagement;
    @FXML private Button btnUserManagement;
    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtShoeCount;
    @FXML private TextField txtSearch;

    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Khởi tạo combobox loại giày
        cbType.getItems().addAll("Nam", "Nữ", "Trẻ em", "Unisex");

        // Liên kết cột với thuộc tính
        colId.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colShoeCount.setCellValueFactory(new PropertyValueFactory<>("shoeCount"));

        // Tạo dữ liệu mẫu
        generateSampleData();

        // Bắt sự kiện chọn hàng
        categoryTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showCategoryDetails(newVal));
    }

    private void generateSampleData() {
        categoryList.addAll(
                new Category("DM01", "Giày thể thao nam", "Nam", 50),
                new Category("DM02", "Giày công sở nữ", "Nữ", 35),
                new Category("DM03", "Giày bóng đá", "Nam", 20),
                new Category("DM04", "Dép sandal", "Nữ", 42),
                new Category("DM05", "Giày trẻ em", "Trẻ em", 28)
        );
        categoryTable.setItems(categoryList);
    }

    private void showCategoryDetails(Category category) {
        if (category != null) {
            txtId.setText(category.getCategoryId());
            txtName.setText(category.getName());
            cbType.setValue(category.getType());
            txtShoeCount.setText(String.valueOf(category.getShoeCount()));
        }
    }

    @FXML
    private void handleAdd() {
        if (validateInput()) {
            Category newCategory = new Category(
                    txtId.getText(),
                    txtName.getText(),
                    cbType.getValue(),
                    Integer.parseInt(txtShoeCount.getText())
            );
            categoryList.add(newCategory);
            clearFields();
        }
    }

    @FXML
    private void handleUpdate() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null && validateInput()) {
            selected.setName(txtName.getText());
            selected.setType(cbType.getValue());
            selected.setShoeCount(Integer.parseInt(txtShoeCount.getText()));
            categoryTable.refresh();
        }
    }

    @FXML
    private void handleDelete() {
        int selectedIndex = categoryTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            categoryList.remove(selectedIndex);
        }
    }

    @FXML
    private void handleClear() {
        clearFields();
        categoryTable.getSelectionModel().clearSelection();
    }

    private boolean validateInput() {
        // Validation logic
        return true;
    }

    private void clearFields() {
        txtId.clear();
        txtName.clear();
        cbType.getSelectionModel().clearSelection();
        txtShoeCount.clear();
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

    public void handleProductManagement(ActionEvent event) {
        Stage stage = (Stage) btnProductManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/ProductManagement.fxml", "Quản lý Sản phẩm");
    }
    @FXML
    public void handleOrderManagement(ActionEvent event) {
        Stage stage = (Stage) btnOrderManagement.getScene().getWindow();
        SceneManager.changeScene(stage, "/com/fxml/OrderManagement.fxml", "Quản lý người dùng");
    }
}
