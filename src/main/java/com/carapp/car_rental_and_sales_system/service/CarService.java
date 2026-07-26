package com.carapp.car_rental_and_sales_system.service;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.storage.DataStore;
import java.util.Collections;   // عشان نستخدم خاصية الترتيب في ترتيب السيارات ابجديا
import java.util.Comparator;    // عشان نقارن بين السيارات لما نيجي نرتبها
import java.util.List;
import java.util.stream.Collectors;     // عشان نقدر نعمل stream

public class CarService {

    private final DataStore dataStore;

    public CarService() {
        this.dataStore = DataStore.getInstance();
    }

    public List<Car> getAllCars() {     // جلب السيارات من ال data Store
        return dataStore.getCars();
    }

    public List<Car> getAvailableCars() {       // تصفية قائمة السيارات من Data Store حسب الحالة
        return dataStore.getCars().stream()     // عملنا stream حديثة احسن ما نقعد نعمل for loops تقليدية
                .filter(Car::isAvailable)
                .collect(Collectors.toList());      // نجمع النتيجة في ليست تاني
    }

    public void addCar(String brand, String model, int year, double pricePerDay, double salePrice, String color, List<String> images) {
        // TEMP ID مؤقت – سيتم إعادة ضبطه بعد الترتيب
        Car newCar = new Car("TEMP", brand, model, year, pricePerDay, salePrice, color, images);
        dataStore.getCars().add(newCar);

        sortCarsAlphabetically();     // ترتيب أبجدي كامل قبل توليد الIDs

        renumberCarIds();           // توليد IDs حسب الترتيب الحالي في الجدول

        dataStore.saveCars();       // عشان نحفظ اي تعديل حصل في الملف
    }

    public void updateCar(Car car) {
        sortCarsAlphabetically();    // في حالة تعديل اسم البراند أو الموديل
        renumberCarIds();
        dataStore.saveCars();
    }

    public void deleteCar(Car car) {
        dataStore.getCars().remove(car);
        renumberCarIds();
        dataStore.saveCars();
    }

    //   الترتيب الأبجدي للبراند بعد كده الموديل
    private void sortCarsAlphabetically() {
        Collections.sort(dataStore.getCars(),
                Comparator.comparing(Car::getBrand, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Car::getModel, String.CASE_INSENSITIVE_ORDER)
        );
    }

    // الـ ID يعكس الترتيب الفعلي داخل الجدول
    private void renumberCarIds() {
        List<Car> cars = dataStore.getCars();
        for (int i = 0; i < cars.size(); i++) {
            cars.get(i).setId("CAR-" + (i + 1));
        }
    }

}
