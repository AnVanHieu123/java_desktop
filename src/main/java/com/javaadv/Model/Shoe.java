package com.javaadv.Model;

import javafx.beans.property.*;

public class Shoe {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty brand;
    private final DoubleProperty price;
    private final IntegerProperty size;
    private final StringProperty color;
    private final IntegerProperty stock;

    public Shoe(String id, String name, String brand, double price, int size, String color, int stock) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.brand = new SimpleStringProperty(brand);
        this.price = new SimpleDoubleProperty(price);
        this.size = new SimpleIntegerProperty(size);
        this.color = new SimpleStringProperty(color);
        this.stock = new SimpleIntegerProperty(stock);
    }

    // Getter methods for properties
    public StringProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty brandProperty() { return brand; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty sizeProperty() { return size; }
    public StringProperty colorProperty() { return color; }
    public IntegerProperty stockProperty() { return stock; }
}