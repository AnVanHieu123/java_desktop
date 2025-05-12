package com.javaadv;

import com.javaadv.Model.Shoe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Random;

public class ShoeController {
    @FXML private TableView<Shoe> shoeTable;
    @FXML private TableColumn<Shoe, String> colId;
    @FXML private TableColumn<Shoe, String> colName;
    @FXML private TableColumn<Shoe, String> colBrand;
    @FXML private TableColumn<Shoe, Double> colPrice;
    @FXML private TableColumn<Shoe, Integer> colSize;
    @FXML private TableColumn<Shoe, String> colColor;
    @FXML private TableColumn<Shoe, Integer> colStock;

    private final ObservableList<Shoe> shoeData = FXCollections.observableArrayList();
    private final Random random = new Random();

    @FXML
    public void initialize() {
        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Generate fake data
        generateFakeShoes(100);

        // Set data to table
        shoeTable.setItems(shoeData);
    }

    private void generateFakeShoes(int count) {
        String[] brands = {"Nike", "Adidas", "Puma", "Converse", "Vans", "New Balance", "Reebok", "Skechers"};
        String[] models = {"Air Max", "Superstar", "Classic", "Old School", "574", "Club C", "Go Walk", "Chuck Taylor"};
        String[] colors = {"Đen", "Trắng", "Đỏ", "Xanh navy", "Xám", "Be", "Xanh lá", "Hồng"};

        for (int i = 1; i <= count; i++) {
            String brand = brands[random.nextInt(brands.length)];
            String model = models[random.nextInt(models.length)];
            String color = colors[random.nextInt(colors.length)];

            shoeData.add(new Shoe(
                    "SH" + String.format("%03d", i), // ID
                    brand + " " + model, // Name
                    brand, // Brand
                    500000 + random.nextInt(3000000), // Price (500k-3.5M)
                    36 + random.nextInt(10), // Size (36-45)
                    color, // Color
                    random.nextInt(50) // Stock (0-49)
            ));
        }
    }
}