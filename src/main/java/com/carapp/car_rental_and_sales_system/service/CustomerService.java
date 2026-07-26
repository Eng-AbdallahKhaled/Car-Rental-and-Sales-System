package com.carapp.car_rental_and_sales_system.service;

import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.storage.DataStore;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerService {

    private final DataStore dataStore;

    public CustomerService() {
        this.dataStore = DataStore.getInstance();
    }
    
    public void activateAllExisting() {
        for (Customer c : dataStore.getCustomers()) {
            c.setActive(true);
        }
        dataStore.saveCustomers();
    }

    public List<Customer> getActiveCustomers() {
        return dataStore.getCustomers().stream()
                .filter(Customer::isActive)
                .collect(Collectors.toList());
    }

    public List<Customer> getArchivedCustomers() {
        return dataStore.getCustomers().stream()
                .filter(c -> !c.isActive())
                .collect(Collectors.toList());
    }

    // ✅ تحديث دالة الإضافة لتشمل النوع والصورة والرقم القومي
    public void addCustomer(String name, String nationalId, String phone, String gender, String imagePath) {
        Customer existing = findCustomerByNationalId(nationalId);
        if (existing == null) {
            String id = "CUST-" + (dataStore.getCustomers().size() + 1);
            dataStore.getCustomers().add(new Customer(id, name, nationalId, phone, gender, imagePath));
        } else {
            existing.setActive(true);
            existing.setName(name);
            existing.setPhone(phone);
            existing.setGender(gender);
            if (imagePath != null && !imagePath.isEmpty()) {
                existing.setImagePath(imagePath);
            }
        }
        dataStore.saveCustomers();
    }

    public void archiveCustomer(Customer c) {
        c.setActive(false);
        dataStore.saveCustomers();
    }

    public void reactivateCustomer(Customer c) {
        c.setActive(true);
        dataStore.saveCustomers();
    }
    
    public void deletePermanent(Customer c) {
        dataStore.getCustomers().remove(c);
        dataStore.saveCustomers();
    }

    public void updateCustomer(Customer customer) {
        dataStore.saveCustomers();
    }

    public Customer findCustomerByNationalId(String nationalId) {
        return dataStore.getCustomers().stream()
                .filter(c -> c.getNationalId() != null && c.getNationalId().equals(nationalId))
                .findFirst()
                .orElse(null);
    }
    
    public Customer findCustomerByPhone(String phone) {
        return dataStore.getCustomers().stream()
                .filter(c -> c.getPhone().equals(phone))
                .findFirst()
                .orElse(null);
    }
}