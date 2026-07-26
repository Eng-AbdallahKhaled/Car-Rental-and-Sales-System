package com.carapp.car_rental_and_sales_system.model;

import java.util.ArrayList;
import java.util.List;

public class Car {

    private String id;
    private String brand;
    private String model;
    private int year;
    private double pricePerDay;
    private double salePrice;
    private boolean available;
    private String color;
    // قائمة الصور
    private List<String> images;

    public Car(String id, String brand, String model, int year, double pricePerDay, double salePrice, String color, List<String> images) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.pricePerDay = pricePerDay;
        this.salePrice = salePrice;
        this.available = true;
        this.color = color;
        // التأكد من أن القائمة ليست فارغة
        this.images = (images != null) ? images : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = (images != null) ? images : new ArrayList<>();
    }

    // دالة توافقية للأكواد القديمة (ترجع أول صورة فقط) عشان تعرض صورة المعاينة 
    public String getImagePath() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }
}
