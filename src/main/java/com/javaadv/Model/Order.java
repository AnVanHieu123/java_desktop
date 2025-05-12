package com.javaadv.Model;

public class Order {
    private String company;
    private String airport;
    private String deliveryDate;
    private String amount;
    private String status;

    // Constructors, getters, setters

    public Order(String company, String status, String amount, String deliveryDate, String airport) {
        this.company = company;
        this.status = status;
        this.amount = amount;
        this.deliveryDate = deliveryDate;
        this.airport = airport;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getAirport() {
        return airport;
    }

    public void setAirport(String airport) {
        this.airport = airport;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }
}