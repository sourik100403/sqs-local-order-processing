package com.example.orderapi.model;

public class OrderRequest {

    private String orderId;
    private String product;
    private double amount;
    private String customerName;
private String email;
private String phone;
private String whatsapp;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
    public String getCustomerName() {
    return customerName;
}

public void setCustomerName(String customerName) {
    this.customerName = customerName;
}

public String getEmail() {
    return email;
}

public void setEmail(String email) {
    this.email = email;
}

public String getPhone() {
    return phone;
}

public void setPhone(String phone) {
    this.phone = phone;
}

public String getWhatsapp() {
    return whatsapp;
}

public void setWhatsapp(String whatsapp) {
    this.whatsapp = whatsapp;
}
}