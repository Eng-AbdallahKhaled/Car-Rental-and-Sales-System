package com.carapp.car_rental_and_sales_system.storage;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import java.util.ArrayList;
import java.util.List;

 // المخزن الرئيسي للبرنامج (Singleton)
 // يحتوي على كل القوائم (Cars, Customers, Rentals, Sales)
 // ويقوم بتحميلها عند البدء وحفظها عند التغيير
 
public class DataStore {

    // أسماء الملفات التي سيتم إنشاؤها بجانب المشروع
    private static final String CARS_FILE = "cars.json";
    private static final String CUSTOMERS_FILE = "customers.json";
    private static final String RENTALS_FILE = "rentals.json";
    private static final String SALES_FILE = "sales.json";

    // القوائم الحقيقية في الذاكرة (RAM)
    private List<Car> cars;
    private List<Customer> customers;
    private List<Rental> rentals;
    private List<Sale> sales;

    // نسخة واحدة من المخزن (Singleton Instance)
    private static DataStore instance;

    // الكونستركتور خاص (Private) لمنع إنشاء نسخ متعددة
    private DataStore() {
        // عند تشغيل المخزن لأول مرة، نحاول تحميل البيانات القديمة
        loadAllData();
    }

    // الطريقة الوحيدة للوصول للمخزن من أي مكان في البرنامج
    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // تحميل البيانات من الملفات 
    private void loadAllData() {
        cars = JsonHandler.load(CARS_FILE, Car.class);
        customers = JsonHandler.load(CUSTOMERS_FILE, Customer.class);
        rentals = JsonHandler.load(RENTALS_FILE, Rental.class);
        sales = JsonHandler.load(SALES_FILE, Sale.class);
    }

    //دوال الحفظ  (يتم استدعاؤها بعد أي إضافة أو تعديل)
    public void saveCars() {
        JsonHandler.save(cars, CARS_FILE);
    }

    public void saveCustomers() {
        JsonHandler.save(customers, CUSTOMERS_FILE);
    }

    public void saveRentals() {
        JsonHandler.save(rentals, RENTALS_FILE);
    }

    public void saveSales() {
        JsonHandler.save(sales, SALES_FILE);
    }

    // Getters للحصول على القوائم 
    public List<Car> getCars() {
        return cars;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public List<Rental> getRentals() {
        return rentals;
    }

    public List<Sale> getSales() {
        return sales;
    }
}
