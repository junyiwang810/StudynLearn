package com.example.model;

public class ProductDetails {
    private final String name;
    private final double price;

    public ProductDetails(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
