package com.carapp.car_rental_and_sales_system.service;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.storage.DataStore;
import java.util.List;

public class SaleService {

    private final DataStore dataStore;

    public SaleService() {
        this.dataStore = DataStore.getInstance();
    }

    public List<Sale> getAllSales() {
        return dataStore.getSales();
    }

    public void sellCar(Car car, Customer customer, double finalPrice) {
        if (!car.isAvailable()) {
            throw new IllegalStateException("Car is not available for sale!");
        }
        String saleId = "SALE-" + (dataStore.getSales().size() + 1);
        Sale sale = new Sale(saleId, car, customer, finalPrice);

        car.setAvailable(false);
        dataStore.getSales().add(sale);
        dataStore.saveSales();
        dataStore.saveCars();
    }

    public void returnCar(Sale sale) {
        if (sale != null) {
            sale.getCar().setAvailable(true);
            dataStore.getSales().remove(sale);
            dataStore.saveSales();
            dataStore.saveCars();
        }
    }
}
