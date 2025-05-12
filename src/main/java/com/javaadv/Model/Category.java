package com.javaadv.Model;

public class Category {
    private String categoryId;  // Mã danh mục (VD: DM01)
    private String name;       // Tên danh mục (VD: Giày thể thao nam)
    private String type;       // Loại (VD: Nam/Nữ/Trẻ em)
    private int shoeCount;     // Số lượng giày trong danh mục

    public Category(String categoryId, String name, String type, int shoeCount) {
        this.categoryId = categoryId;
        this.name = name;
        this.type = type;
        this.shoeCount = shoeCount;
    }

    // Getter/Setter
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getShoeCount() { return shoeCount; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setShoeCount(int shoeCount) { this.shoeCount = shoeCount; }
}