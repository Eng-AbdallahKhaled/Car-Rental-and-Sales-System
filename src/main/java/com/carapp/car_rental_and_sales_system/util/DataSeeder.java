package com.carapp.car_rental_and_sales_system.util;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.storage.DataStore; 
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DataSeeder {

    private final CarService carService = new CarService();
    private final CustomerService customerService = new CustomerService();
    private final RentalService rentalService = new RentalService();
    private final SaleService saleService = new SaleService();
    private final Random random = new Random();

    private static final String[] ELITE_CARS = {
        "Bugatti_Chiron", "Bugatti_Veyron", "Bugatti_Divo", "Bugatti_Centodieci", "Bugatti_Bolide", 
        "Bugatti_La_Voiture_Noire", "Bugatti_Mistral", "Bugatti_Eb110", "Bugatti_Type_35", "Bugatti_Galibier",
        "Ferrari_LaFerrari", "Ferrari_Enzo", "Ferrari_F40", "Ferrari_F50", "Ferrari_SF90_Stradale", 
        "Ferrari_488_Pista", "Ferrari_812_Competizione", "Ferrari_Roma", "Ferrari_Portofino", "Ferrari_Daytona_SP3", 
        "Ferrari_Purosangue", "Ferrari_296_GTS", "Ferrari_Monza_SP1", "Ferrari_GTC4Lusso", "Ferrari_F12_tdf",
        "Lamborghini_Aventador_SVJ", "Lamborghini_Huracan_STO", "Lamborghini_Urus_Performante", "Lamborghini_Sian", "Lamborghini_Countach_LPI", 
        "Lamborghini_Revuelto", "Lamborghini_Murcielago_SV", "Lamborghini_Diablo_GT", "Lamborghini_Gallardo", "Lamborghini_Centenario", 
        "Lamborghini_Veneno", "Lamborghini_Reventon", "Lamborghini_Miura", "Lamborghini_Egoista", "Lamborghini_Estoque",
        "Porsche_918_Spyder", "Porsche_Carrera_GT", "Porsche_911_GT3_RS", "Porsche_911_Turbo_S", "Porsche_Taycan", 
        "Porsche_Panamera", "Porsche_Cayenne_Turbo", "Porsche_Macan_GTS", "Porsche_718_Cayman_GT4", "Porsche_959",
        "McLaren_P1", "McLaren_Senna", "McLaren_720S", "McLaren_765LT", "McLaren_Artura", 
        "McLaren_Elva", "McLaren_Speedtail", "McLaren_GT", "McLaren_F1", "McLaren_Solus_GT",
        "Rolls_Royce_Phantom", "Rolls_Royce_Cullinan", "Rolls_Royce_Spectre", "Rolls_Royce_Ghost", "Rolls_Royce_Wraith", 
        "Rolls_Royce_Dawn", "Rolls_Royce_Sweptail", "Rolls_Royce_Boat_Tail", "Rolls_Royce_Droptail", "Rolls_Royce_Silver_Ghost",
        "Bentley_Continental_GT", "Bentley_Flying_Spur", "Bentley_Bentayga", "Bentley_Mulsanne", "Bentley_Bacalar", 
        "Bentley_Batur", "Bentley_Brooklands", "Bentley_Azure", "Bentley_Arnage", "Bentley_Turbo_R",
        "Aston_Martin_DBS", "Aston_Martin_Valkyrie", "Aston_Martin_Valhalla", "Aston_Martin_DB12", "Aston_Martin_DBX707", 
        "Aston_Martin_Vantage", "Aston_Martin_One77", "Aston_Martin_Vulcan", "Aston_Martin_Victor", "Aston_Martin_Speedster",
        "Mercedes_AMG_G63", "Mercedes_AMG_GT_Black_Series", "Mercedes_SLS_AMG", "Mercedes_Maybach_S680", "Mercedes_AMG_One", 
        "Mercedes_AMG_SL63", "Mercedes_Maybach_Exelero", "Mercedes_Maybach_GLS600", "Mercedes_EQS_Sedan", "Mercedes_CLK_GTR",
        "BMW_XM", "BMW_i8", "BMW_M8_Competition", "BMW_M4_CSL", "BMW_M5_CS", 
        "Audi_R8_V10", "Audi_RS_e_tron_GT", "Audi_RS7", "Audi_RS6_Avant", "Audi_TT_RS"
    };

    public void run() {
        System.out.println("🚀 Starting Elite Data Seeding...");
        clearDatabase();
        List<Car> generatedCars = seedCars();
        List<Customer> generatedCustomers = seedCustomers();
        seedTransactions(generatedCars, generatedCustomers);
        System.out.println("✅ Data Seeding Completed!");
    }
    
    private void clearDatabase() {
        System.out.println("⚠️ Clearing previous database records...");
        DataStore ds = DataStore.getInstance();
        ds.getCars().clear();
        ds.getCustomers().clear();
        ds.getRentals().clear();
        ds.getSales().clear();
        
        ds.saveCars();
        ds.saveCustomers();
        ds.saveRentals();
        ds.saveSales();
        System.out.println("✅ Database cleared.");
    }

    private List<Car> seedCars() {
        File imagesDir = new File("car_images");
        for (String carNameRaw : ELITE_CARS) {
            String brand;
            String model;

            if (carNameRaw.startsWith("Aston_Martin")) {
                brand = "Aston Martin";
                model = carNameRaw.substring("Aston_Martin_".length()).replace("_", " ");
            } else if (carNameRaw.startsWith("Rolls_Royce")) {
                brand = "Rolls-Royce";
                model = carNameRaw.substring("Rolls_Royce_".length()).replace("_", " ");
            } else if (carNameRaw.startsWith("Mercedes_AMG")) {
                brand = "Mercedes-AMG";
                model = carNameRaw.substring("Mercedes_AMG_".length()).replace("_", " ");
            } else if (carNameRaw.startsWith("Mercedes_Maybach")) {
                brand = "Mercedes-Maybach";
                model = carNameRaw.substring("Mercedes_Maybach_".length()).replace("_", " ");
            } else {
                brand = carNameRaw.split("_")[0];
                model = carNameRaw.replace(brand + "_", "").replace("_", " ");
            }
            
            int year = 2020 + random.nextInt(5); 
            double rawBasePrice = 200000 + (random.nextDouble() * 4000000); 
            double basePrice = Math.round(rawBasePrice * 100.0) / 100.0;
            double rawRentPrice = basePrice * 0.008; 
            double rentPrice = Math.round(rawRentPrice * 100.0) / 100.0;
            
            String[] colors = {"Black", "White", "Silver", "Red", "Blue", "Yellow", "Green", "Orange", "Matte Black", "Gold"};
            String color = colors[random.nextInt(colors.length)];

            List<String> images = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                File imgFile = new File(imagesDir, carNameRaw + "_" + i + ".jpg");
                if (imgFile.exists()) {
                    images.add("car_images/" + imgFile.getName());
                }
            }

            carService.addCar(brand, model, year, rentPrice, basePrice, color, images);
        }
        return DataStore.getInstance().getCars();
    }

    private List<Customer> seedCustomers() {
        String[] firstNames = {"Ahmed", "Mohamed", "Ali", "Omar", "Youssef", "Ibrahim", "Khaled", "Hassan", "Mahmoud", "Amr", "Ziad", "Mostafa", "Tarek", "Kareem"};
        String[] lastNames = {"El-Sayed", "Hassan", "Ali", "Ibrahim", "Mostafa", "Mohamed", "Ahmed", "Saad", "Abdallah", "Saleh", "Fawzy", "Kamel"};
        String[] genders = {"Male", "Female"};
        
        for (int i = 0; i < 50; i++) {
            String name = firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
            
            StringBuilder nidBuilder = new StringBuilder();
            nidBuilder.append(random.nextBoolean() ? "2" : "3");
            nidBuilder.append(String.format("%02d", random.nextInt(99))); 
            nidBuilder.append(String.format("%02d", random.nextInt(12) + 1)); 
            nidBuilder.append(String.format("%02d", random.nextInt(28) + 1)); 
            for(int j=0; j<7; j++) nidBuilder.append(random.nextInt(10)); 
            String nationalId = nidBuilder.toString();

            String phone = "01" + random.nextInt(3) + String.format("%08d", random.nextInt(100000000));
            String gender = genders[random.nextInt(genders.length)];
            
            // ✅ إنشاء عملاء وهميين بنوع عشوائي وبدون صورة في البداية
            customerService.addCustomer(name, nationalId, phone, gender, null); 
        }
        return DataStore.getInstance().getCustomers();
    }

    private void seedTransactions(List<Car> cars, List<Customer> customers) {
        if (cars.isEmpty() || customers.isEmpty()) return;

        List<Car> shuffledCars = new ArrayList<>(cars);
        Collections.shuffle(shuffledCars);
        DataStore ds = DataStore.getInstance();
        
        int rentalCount = 0;
        int salesCount = 0;
        int index = 0;

        while (rentalCount < 25 && index < shuffledCars.size()) {
            Car car = shuffledCars.get(index++);
            Customer customer = customers.get(random.nextInt(customers.size()));
            LocalDate start = LocalDate.now().minusDays(random.nextInt(30));
            LocalDate end = start.plusDays(3 + random.nextInt(10));
            
            try {
                rentalService.rentCar(car, customer, start, end);
                List<Rental> allRentals = ds.getRentals();
                Rental lastRental = allRentals.get(allRentals.size() - 1);
                lastRental.setContractDate(start); 
                rentalCount++;
            } catch (Exception e) {}
        }
        ds.saveRentals();

        while (salesCount < 25 && index < shuffledCars.size()) {
            Car car = shuffledCars.get(index++);
            Customer customer = customers.get(random.nextInt(customers.size()));
            
            String saleId = "SALE-" + (ds.getSales().size() + 1);
            LocalDate saleDate = LocalDate.now().minusDays(random.nextInt(90));
            
            Sale sale = new Sale(saleId, car, customer, car.getSalePrice());
            sale.setSaleDate(saleDate);
            
            car.setAvailable(false);
            ds.getSales().add(sale);
            salesCount++;
        }
        ds.saveSales();
        ds.saveCars();
    }
}