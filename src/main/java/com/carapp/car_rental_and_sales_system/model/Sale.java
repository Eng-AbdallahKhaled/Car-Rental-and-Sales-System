package com.carapp.car_rental_and_sales_system.model;

import java.time.LocalDate;

public class Sale {

    private String saleId;
    private Car car;
    private Customer customer;
    private double price;
    private LocalDate saleDate;

    public Sale(String saleId, Car car, Customer customer, double price) {
        this.saleId = saleId;
        this.car = car;
        this.customer = customer;
        this.price = price;
        this.saleDate = LocalDate.now(); // التاريخ الافتراضي هو اليوم
    }

    public String getSaleId() {
        return saleId;
    }

    public Car getCar() {
        return car;
    }

    public Customer getCustomer() {
        return customer;
    }

    public double getPrice() {
        return price;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
}
