package com.javaadv.Model;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();
    private final StringProperty updatedAt = new SimpleStringProperty();

    public User() {
    }

    public User(int id, String fullName, String email, String phone, String status, String address, String role, String createdAt, String updatedAt) {
        this.id.set(id);
        this.fullName.set(fullName);
        this.email.set(email);
        this.phone.set(phone);
        this.status.set(status);
        this.address.set(address);
        this.role.set(role);
        this.createdAt.set(createdAt);
        this.updatedAt.set(updatedAt);
    }

    // Getters và Setters cho id
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // Getters và Setters cho fullName
    public String getFullName() {
        return fullName.get();
    }

    public void setFullName(String fullName) {
        this.fullName.set(fullName);
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    // Getters và Setters cho email
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    // Getters và Setters cho phone
    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String phone) {
        this.phone.set(phone);
    }

    public StringProperty phoneProperty() {
        return phone;
    }

    // Getters và Setters cho status
    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public StringProperty statusProperty() {
        return status;
    }

    // Getters và Setters cho address
    public String getAddress() {
        return address.get();
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public StringProperty addressProperty() {
        return address;
    }

    // Getters và Setters cho role
    public String getRole() {
        return role.get();
    }

    public void setRole(String role) {
        this.role.set(role);
    }

    public StringProperty roleProperty() {
        return role;
    }

    // Getters và Setters cho createdAt
    public String getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt.set(createdAt);
    }

    public StringProperty createdAtProperty() {
        return createdAt;
    }

    // Getters và Setters cho updatedAt
    public String getUpdatedAt() {
        return updatedAt.get();
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt.set(updatedAt);
    }

    public StringProperty updatedAtProperty() {
        return updatedAt;
    }
}