package com.carapp.car_rental_and_sales_system.model;

public class Customer {

    private String id;
    private String name;
    private String nationalId;
    private String phone;
    private String gender; // ✅ تم إضافة النوع
    private String imagePath; // ✅ تم إضافة مسار الصورة
    private boolean active;

    public Customer(String id, String name, String nationalId, String phone, String gender, String imagePath) {
        this.id = id;
        this.name = name;
        this.nationalId = nationalId;
        this.phone = phone;
        this.gender = gender;
        this.imagePath = imagePath;
        this.active = true; // الافتراضي أن العميل نشط عند الإنشاء
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}