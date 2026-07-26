package com.carapp.car_rental_and_sales_system.service;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.storage.DataStore;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;   // عشان نحسب عدد الايام
import java.util.List;

public class RentalService {

    private final DataStore dataStore;

    public RentalService() {
        this.dataStore = DataStore.getInstance();
    }

    public List<Rental> getAllRentals() {
        return dataStore.getRentals();
    }

    public void rentCar(Car car, Customer customer, LocalDate startDate, LocalDate endDate) {
        // بيتحقق لو السيارة متباعة
        boolean isSold = dataStore.getSales().stream()
                .anyMatch(s -> s.getCar().getId().equals(car.getId()));

        if (isSold) {
            throw new IllegalStateException("Car is sold and not available for rent!");
        }

        // منع الحجز في الماضي
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past!");
        }

        //بيتحقق لو في تعارض في التواريخ مع إيجار آخر
        boolean hasOverlap = dataStore.getRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .anyMatch(r -> {
                    // شرط التداخل (StartA <= EndB) and (EndA >= StartB)
                    // الشرط ده بيمنع التداخل ويسمح بالحجز في الفترات الحرة قبل أو بعد الحجز الآخر
                    return !startDate.isAfter(r.getEndDate()) && !endDate.isBefore(r.getStartDate());
                });

        if (hasOverlap) {
            throw new IllegalStateException("Car is already reserved for the selected dates!");
        }

        // إتمام الإيجار
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days == 0) {
            days = 1;
        }
        double totalPrice = days * car.getPricePerDay();
        String rentalId = "RENT-" + (dataStore.getRentals().size() + 1);

        Rental rental = new Rental(rentalId, car, customer, startDate, endDate, totalPrice);

        // تحديث حالة السيارة فقط إذا كان الإيجار يبدأ اليوم
        if (!startDate.isAfter(LocalDate.now())) {
            car.setAvailable(false);
        }

        dataStore.getRentals().add(rental);
        dataStore.saveRentals();
        dataStore.saveCars();
    }

    // دالة الإرجاع (مع حساب الاسترجاع النسبي)
    public double returnCar(Rental rental) {
        if (rental == null) {
            return 0.0;
        }

        rental.getCar().setAvailable(true); // إعادة الإتاحة
        dataStore.getRentals().remove(rental);
        dataStore.saveRentals();
        dataStore.saveCars();

        // حساب الاسترجاع
        LocalDate today = LocalDate.now();
        LocalDate startDate = rental.getStartDate();
        LocalDate endDate = rental.getEndDate();

        if (today.isBefore(startDate)) {
            return rental.getTotalPrice(); // لم يبدأ بعد، استرجاع كامل
        }
        if (today.isAfter(endDate) || today.isEqual(endDate)) {
            return 0.0; // انتهى بالفعل
        }

        long daysUsed = ChronoUnit.DAYS.between(startDate, today);
        if (daysUsed < 1) {
            daysUsed = 1;
        }
        double costUsed = daysUsed * rental.getCar().getPricePerDay();

        return Math.max(0.0, rental.getTotalPrice() - costUsed);
    }
}
