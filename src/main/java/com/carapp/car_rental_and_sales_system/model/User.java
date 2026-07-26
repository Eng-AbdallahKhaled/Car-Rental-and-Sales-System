package com.carapp.car_rental_and_sales_system.model;

public class User {

    private String username;
    private String password;
    private String role;
    private String passwordHint; // التلميح

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.passwordHint = ""; // افتراضيا فاضي
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHint() {
        return passwordHint;
    }

    public void setPasswordHint(String passwordHint) {
        this.passwordHint = passwordHint;
    }
}
