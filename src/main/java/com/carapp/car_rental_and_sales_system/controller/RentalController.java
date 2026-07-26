package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.util.ExportUtil; // ✅ استدعاء كلاس الإكسيل
import com.carapp.car_rental_and_sales_system.util.InvoiceGenerator;
import java.awt.Desktop;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class RentalController {

    @FXML
    private VBox rootPane;
    @FXML
    private TextField txtSearchCar;
    @FXML
    private TextField txtSearchCustomer;
    @FXML
    private TextField txtSearchTable;

    @FXML
    private ComboBox<Car> comboCars;
    @FXML
    private ComboBox<Customer> comboCustomers;
    @FXML
    private DatePicker dateStart;
    @FXML
    private DatePicker dateEnd;
    @FXML
    private Label lblTotalPrice;

    @FXML
    private Button btnShowData;
    @FXML
    private Button btnShowImages;
    @FXML
    private Button btnReturnCar;
    @FXML
    private Button btnTableShowData;
    @FXML
    private Button btnTableShowImages;
    @FXML
    private Button btnOpenInvoice;
    @FXML
    private Button btnExportExcel; // ✅ زرار الإكسيل الجديد

    @FXML
    private ImageView imgCarPreview;

    @FXML
    private TableView<Rental> tableRentals;
    @FXML
    private TableColumn<Rental, String> colId;
    @FXML
    private TableColumn<Rental, String> colCar;
    @FXML
    private TableColumn<Rental, String> colCustomer;
    @FXML
    private TableColumn<Rental, LocalDate> colStart;
    @FXML
    private TableColumn<Rental, LocalDate> colEnd;
    @FXML
    private TableColumn<Rental, Double> colPrice;
    @FXML
    private TableColumn<Rental, LocalDate> colContractDate;

    private final RentalService rentalService;
    private final CarService carService;
    private final CustomerService customerService;
    private final SaleService saleService;

    private FilteredList<Car> filteredCars;
    private FilteredList<Customer> filteredCustomers;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public RentalController() {
        this.rentalService = new RentalService();
        this.carService = new CarService();
        this.customerService = new CustomerService();
        this.saleService = new SaleService();
    }

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            tableRentals.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

            loadData();
            setupComboBoxes();
            setupSearchFilters();

            // إعداد قيود التاريخ الأولية
            updateDateCells(null);

            // عند تغيير تاريخ البدء، نحدث قيود تاريخ الانتهاء
            dateStart.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateDateCells(comboCars.getValue());
                calculatePrice();
            });

            // عند تغيير تاريخ الانتهاء، نحسب السعر
            dateEnd.valueProperty().addListener((obs, old, newV) -> calculatePrice());

            // الحالة الأولية للأزرار
            updateViewButtonsState(null);
            if (btnReturnCar != null) {
                btnReturnCar.setDisable(true);
            }
            if (btnTableShowData != null) {
                btnTableShowData.setDisable(true);
            }
            if (btnTableShowImages != null) {
                btnTableShowImages.setDisable(true);
            }

            if (btnOpenInvoice != null) {
                btnOpenInvoice.setDisable(true);
                btnOpenInvoice.setText("View Contract");
            }

            // تمييز الأيام المختلفة في الجدول
            tableRentals.setRowFactory(tv -> new TableRow<Rental>() {
                @Override
                protected void updateItem(Rental item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                    } else {
                        int currentIndex = getIndex();
                        String style = "";
                        if (currentIndex > 0 && currentIndex < getTableView().getItems().size()) {
                            Rental previousItem = getTableView().getItems().get(currentIndex - 1);
                            if (item.getContractDate() != null && previousItem.getContractDate() != null) {
                                if (!item.getContractDate().isEqual(previousItem.getContractDate())) {
                                    style = "-fx-border-color: #fcd513; -fx-border-width: 2 0 0 0;";
                                }
                            }
                        }
                        setStyle(style);
                    }
                }
            });

            tableRentals.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                boolean hasSelection = (newVal != null);
                if (btnReturnCar != null) {
                    btnReturnCar.setDisable(!hasSelection);
                }
                if (btnTableShowData != null) {
                    btnTableShowData.setDisable(!hasSelection);
                }

                if (btnOpenInvoice != null) {
                    btnOpenInvoice.setDisable(!hasSelection || findPdf(newVal) == null);
                }

                if (btnTableShowImages != null) {
                    if (hasSelection && newVal.getCar().getImages() != null && !newVal.getCar().getImages().isEmpty()) {
                        btnTableShowImages.setDisable(false);
                    } else {
                        btnTableShowImages.setDisable(true);
                    }
                }

                if (newVal != null) {
                    updateImagePreview(newVal.getCar());
                } else {
                    updateImagePreview(null);
                }
            });

            comboCars.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                calculatePrice();
                updateViewButtonsState(newVal);
                updateImagePreview(newVal);
                // تحديث قيود التاريخ (Start/End) بناءً على السيارة المختارة
                updateDateCells(newVal);
            });

            if (rootPane != null) {
                rootPane.setOnMousePressed(event -> rootPane.requestFocus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("System Error", "Controller initialization failed:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void calculatePrice() {
        if (lblTotalPrice == null) {
            return;
        }
        Car car = comboCars.getValue();
        LocalDate start = dateStart.getValue();
        LocalDate end = dateEnd.getValue();

        if (car != null && start != null && end != null && !end.isBefore(start)) {
            long days = ChronoUnit.DAYS.between(start, end);
            if (days == 0) {
                days = 1;
            }
            double total = days * car.getPricePerDay();
            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
            lblTotalPrice.setText(currency.format(total));
        } else {
            lblTotalPrice.setText("...");
        }
    }

    private String computeCarStatusShort(Car car) {
        if (car == null) {
            return "";
        }
        LocalDate today = LocalDate.now();
        boolean isSold = saleService.getAllSales().stream().anyMatch(s -> s.getCar().getId().equals(car.getId()));
        if (isSold) {
            return "❌ Sold";
        }

        Optional<Rental> now = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .filter(r -> !today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate()))
                .findFirst();
        if (now.isPresent()) {
            return "🚫 Rented until: " + now.get().getEndDate().format(DF);
        }

        Optional<Rental> future = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .filter(r -> r.getStartDate().isAfter(today))
                .min(Comparator.comparing(Rental::getStartDate));
        if (future.isPresent()) {
            return "⚠️ Available until: " + future.get().getStartDate().minusDays(1).format(DF);
        }

        return "✅ Available Now";
    }

    private void updateDateCells(Car selectedCar) {
        if (selectedCar == null) {
            dateStart.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                    }
                }
            });
            dateEnd.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate start = dateStart.getValue();
                    if ((start != null && date.isBefore(start)) || (start == null && date.isBefore(LocalDate.now()))) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                    }
                }
            });
            return;
        }

        List<Rental> carRentals = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(selectedCar.getId()))
                .collect(Collectors.toList());

        dateStart.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    return;
                }

                boolean insideExisting = carRentals.stream()
                        .anyMatch(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()));
                if (insideExisting) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    setTooltip(new Tooltip("Conflicts with existing booking"));
                    return;
                }
            }
        });

        dateEnd.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate start = dateStart.getValue();

                if ((start != null && date.isBefore(start)) || (start == null && date.isBefore(LocalDate.now()))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    return;
                }

                if (start != null) {
                    boolean overlaps = carRentals.stream()
                            .anyMatch(r -> !(date.isBefore(r.getStartDate()) || start.isAfter(r.getEndDate())));
                    if (overlaps) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Overlaps existing booking"));
                        return;
                    }
                } else {
                    boolean insideExisting = carRentals.stream()
                            .anyMatch(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()));
                    if (insideExisting) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Conflicts with existing booking"));
                        return;
                    }
                }
            }
        });
    }

    private LocalDate getNextBookingStart(Car car) {
        return rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .filter(r -> r.getStartDate().isAfter(LocalDate.now())) 
                .map(Rental::getStartDate)
                .sorted() 
                .findFirst()
                .orElse(null);
    }

    private void loadData() {
        List<Rental> allRentals = new ArrayList<>(rentalService.getAllRentals());
        allRentals.sort(Comparator.comparing(Rental::getContractDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        ObservableList<Rental> rentalList = FXCollections.observableArrayList(allRentals);
        FilteredList<Rental> filteredRentals = new FilteredList<>(rentalList, b -> true);

        if (txtSearchTable != null) {
            txtSearchTable.textProperty().addListener((o, ol, n) -> filteredRentals.setPredicate(r -> {
                if (n == null || n.isEmpty()) {
                    return true;
                }
                String f = n.toLowerCase();
                String d = (r.getRentalId() + " " + r.getCar().getBrand() + " " + r.getCustomer().getName()).toLowerCase();
                for (String w : d.split(" ")) {
                    if (w.startsWith(f)) {
                        return true;
                    }
                }
                return false;
            }));
        }

        SortedList<Rental> sortedData = new SortedList<>(filteredRentals);
        sortedData.comparatorProperty().bind(tableRentals.comparatorProperty());
        tableRentals.setItems(sortedData);

        List<Car> carsForCombo = carService.getAllCars().stream()
                .filter(c -> {
                    boolean sold = saleService.getAllSales().stream().anyMatch(s -> s.getCar().getId().equals(c.getId()));
                    return !sold;
                })
                .sorted(Comparator.comparing(Car::getBrand, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Car::getModel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        filteredCars = new FilteredList<>(FXCollections.observableArrayList(carsForCombo), p -> true);
        comboCars.setItems(filteredCars);

        List<Customer> sortedCustomers = new ArrayList<>(customerService.getActiveCustomers());
        sortedCustomers.sort(Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER));

        ObservableList<Customer> activeCustomers = FXCollections.observableArrayList(sortedCustomers);
        filteredCustomers = new FilteredList<>(activeCustomers, p -> true);
        comboCustomers.setItems(filteredCustomers);
    }

    private void updateImagePreview(Car car) {
        if (imgCarPreview == null) {
            return;
        }

        if (car != null && car.getImages() != null && !car.getImages().isEmpty()) {
            try {
                File file = new File(car.getImages().get(0));
                if (file.exists()) {
                    imgCarPreview.setImage(new Image(file.toURI().toString()));
                } else {
                    imgCarPreview.setImage(null);
                }
            } catch (Exception e) {
                imgCarPreview.setImage(null);
            }
        } else {
            imgCarPreview.setImage(null);
        }
    }

    private File findPdf(Rental rental) {
        if (rental == null) {
            return null;
        }
        File dir = new File("invoices/Rentals");
        if (!dir.exists() || dir.listFiles() == null) {
            return null;
        }
        for (File f : dir.listFiles()) {
            if (f.getName().toLowerCase().endsWith(".pdf") && f.getName().contains(rental.getRentalId())) {
                return f;
            }
        }
        return null;
    }

    // ✅ دالة التصدير للإكسيل الخاصة بجدول الإيجارات
    @FXML
    void btnExportToExcelAction(ActionEvent event) {
        ExportUtil.exportTableToExcel(tableRentals, txtSearchTable.getScene().getWindow(), "Rentals_Data");
    }

    @FXML
    void btnTableShowDataAction(ActionEvent event) {
        try {
            Rental rental = (tableRentals != null) ? tableRentals.getSelectionModel().getSelectedItem() : null;
            if (rental != null) {
                showCarDataDialog(rental.getCar());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnTableShowImagesAction(ActionEvent event) {
        try {
            Rental rental = (tableRentals != null) ? tableRentals.getSelectionModel().getSelectedItem() : null;
            if (rental != null) {
                showCarImagesWindow(rental.getCar());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnShowDataAction(ActionEvent event) {
        try {
            Car car = (comboCars != null) ? comboCars.getValue() : null;
            if (car != null) {
                showCarDataDialog(car);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnShowImagesAction(ActionEvent event) {
        try {
            Car car = (comboCars != null) ? comboCars.getValue() : null;
            if (car != null) {
                showCarImagesWindow(car);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnShowDataAction() {
        btnShowDataAction(new javafx.event.ActionEvent());
    }

    @FXML
    void btnShowImagesAction() {
        btnShowImagesAction(new javafx.event.ActionEvent());
    }

    @FXML
    void btnTableShowDataAction() {
        btnTableShowDataAction(new javafx.event.ActionEvent());
    }

    @FXML
    void btnTableShowImagesAction() {
        btnTableShowImagesAction(new javafx.event.ActionEvent());
    }

    @FXML
    void btnOpenInvoiceAction(ActionEvent event) {
        Rental rental = tableRentals.getSelectionModel().getSelectedItem();
        File pdfFile = findPdf(rental);
        if (pdfFile != null) {
            try {
                Desktop.getDesktop().open(pdfFile);
            } catch (Exception e) {
                showAlert("Error", "Could not open PDF file.");
            }
        }
    }

    @FXML
    void btnRentCar(ActionEvent event) {
        try {
            Car selectedCar = comboCars.getValue();
            Customer selectedCustomer = comboCustomers.getValue();
            LocalDate start = dateStart.getValue();
            LocalDate end = dateEnd.getValue();

            if (selectedCar == null || selectedCustomer == null || start == null || end == null) {
                showAlert("Error", "Please fill all fields!");
                return;
            }
            if (end.isBefore(start)) {
                showAlert("Error", "End date cannot be before start date!");
                return;
            }
            if (start.isBefore(LocalDate.now())) {
                showAlert("Error", "Start date cannot be in the past!");
                return;
            }

            List<Rental> existing = rentalService.getAllRentals().stream()
                    .filter(r -> r.getCar().getId().equals(selectedCar.getId()))
                    .collect(Collectors.toList());
            boolean overlap = existing.stream()
                    .anyMatch(r -> !(end.isBefore(r.getStartDate()) || start.isAfter(r.getEndDate())));
            if (overlap) {
                showAlert("Error", "Selected period overlaps existing booking. Choose different dates.");
                return;
            }

            long days = ChronoUnit.DAYS.between(start, end);
            if (days == 0) {
                days = 1;
            }
            double totalPrice = days * selectedCar.getPricePerDay();

            rentalService.rentCar(selectedCar, selectedCustomer, start, end);
            showAlert("Success", "Car Rented Successfully!");

            List<Rental> rentals = rentalService.getAllRentals();
            Rental newRental = rentals.get(rentals.size() - 1);
            InvoiceGenerator.generateRentalInvoice(newRental.getRentalId(), selectedCar, selectedCustomer, totalPrice, start, end);

            loadData();

            comboCars.getSelectionModel().clearSelection();
            comboCustomers.getSelectionModel().clearSelection();
            dateStart.setValue(null);
            dateEnd.setValue(null);
            if (lblTotalPrice != null) {
                lblTotalPrice.setText("0.0");
            }
            updateViewButtonsState(null);

            txtSearchCar.clear();
            txtSearchCustomer.clear();
            if (txtSearchTable != null) {
                txtSearchTable.clear();
            }

        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    void btnReturnCarAction(ActionEvent event) {
        Rental rental = tableRentals.getSelectionModel().getSelectedItem();
        if (rental == null) {
            return;
        }

        rentalService.returnCar(rental);
        showAlert("Success", "Car returned successfully!");

        InvoiceGenerator.generateReturnReceipt(rental.getRentalId(), rental.getCar(), rental.getCustomer());

        loadData();

        txtSearchCar.clear();
        txtSearchCustomer.clear();
        if (txtSearchTable != null) {
            txtSearchTable.clear();
        }
    }

    private void setupSearchFilters() {
        txtSearchCar.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredCars.setPredicate(car -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerFilter = newVal.toLowerCase();
                String carData = (car.getBrand() + " " + car.getModel()).toLowerCase();
                String[] words = carData.split(" ");
                for (String word : words) {
                    if (word.startsWith(lowerFilter)) {
                        return true;
                    }
                }
                if (carData.startsWith(lowerFilter)) {
                    return true;
                }
                return false;
            });
            if (!filteredCars.isEmpty() && newVal != null && !newVal.isEmpty()) {
                if (!comboCars.isShowing()) {
                    comboCars.show();
                }
            }
            if (filteredCars.size() == 1) {
                comboCars.getSelectionModel().selectFirst();
            }
        });

        txtSearchCustomer.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredCustomers.setPredicate(cust -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerFilter = newVal.toLowerCase();
                String name = cust.getName().toLowerCase();
                String[] parts = name.split(" ");
                for (String part : parts) {
                    if (part.startsWith(lowerFilter)) {
                        return true;
                    }
                }
                // ✅ فلترة باسم العميل أو هاتفه أو رقمه القومي
                if (cust.getPhone().contains(lowerFilter) || (cust.getNationalId() != null && cust.getNationalId().contains(lowerFilter))) {
                    return true;
                }
                return false;
            });
            if (!filteredCustomers.isEmpty() && newVal != null && !newVal.isEmpty()) {
                if (!comboCustomers.isShowing()) {
                    comboCustomers.show();
                }
            }
            if (filteredCustomers.size() == 1) {
                comboCustomers.getSelectionModel().selectFirst();
            }
        });
    }

    private void updateViewButtonsState(Car car) {
        if (car != null) {
            if (btnShowData != null) {
                btnShowData.setDisable(false);
            }
            if (btnShowImages != null) {
                boolean hasImages = car.getImages() != null && !car.getImages().isEmpty();
                btnShowImages.setDisable(!hasImages);
            }
        } else {
            if (btnShowData != null) {
                btnShowData.setDisable(true);
            }
            if (btnShowImages != null) {
                btnShowImages.setDisable(true);
            }
        }
    }

    private void setupTableColumns() {
        colContractDate.setCellValueFactory(new PropertyValueFactory<>("contractDate"));
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRentalId()));
        colCar.setCellValueFactory(cell -> {
            Car c = cell.getValue().getCar();
            return new SimpleStringProperty(c.getBrand() + " " + c.getModel());
        });
        colCustomer.setCellValueFactory(cell -> {
            Customer c = cell.getValue().getCustomer();
            return new SimpleStringProperty(c.getName());
        });
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTotalPrice()));
        formatCurrencyColumn(colPrice);
    }

    private void formatCurrencyColumn(TableColumn<Rental, Double> column) {
        column.setCellFactory(tc -> new TableCell<Rental, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
                    setText(currencyFormat.format(price));
                }
            }
        });
    }

    private void setupComboBoxes() {
        Callback<ListView<Car>, ListCell<Car>> carCellFactory = param -> new ListCell<Car>() {
            private final HBox hbox = new HBox(8);
            private final Label lblName = new Label();
            private final Region spacer = new Region();
            private final Label lblStatus = new Label();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius:6;");
                lblStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: black;");
                hbox.getChildren().addAll(lblName, spacer, lblStatus);
            }

            @Override
            protected void updateItem(Car item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lblName.setText(item.getBrand() + " " + item.getModel());
                    String status = computeCarStatusShort(item);
                    lblStatus.setText(status);

                    if (status.startsWith("❌")) {
                        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius:6; -fx-background-color: rgba(231,76,60,0.95);");
                    } else if (status.startsWith("🚫")) {
                        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius:6; -fx-background-color: rgba(155,89,182,0.95);");
                    } else if (status.startsWith("⚠️")) {
                        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: black; -fx-padding: 4 8; -fx-background-radius:6; -fx-background-color: rgba(241,196,15,0.95);");
                    } else {
                        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius:6; -fx-background-color: rgba(46,204,113,0.95);");
                    }

                    setGraphic(hbox);
                }
            }
        };

        comboCars.setCellFactory(carCellFactory);
        comboCars.setButtonCell(carCellFactory.call(null));

        comboCars.setButtonCell(new ListCell<Car>() {
            @Override
            protected void updateItem(Car item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setGraphic(null);
                    setText(item.getBrand() + " " + item.getModel());
                    setStyle("-fx-text-fill: black; -fx-font-size: 13px; -fx-padding: 4 8;");
                }
            }
        });

        Callback<ListView<Customer>, ListCell<Customer>> custCellFactory = param -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        };
        comboCustomers.setButtonCell(custCellFactory.call(null));
        comboCustomers.setCellFactory(custCellFactory);
    }

    private void showCarDataDialog(Car car) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Car Information");
        dialog.setHeaderText("Details for: " + car.getBrand() + " " + car.getModel());
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        content.getChildren().add(createDetailLabel("Year: ", String.valueOf(car.getYear())));
        content.getChildren().add(createDetailLabel("Color: ", car.getColor()));
        content.getChildren().add(createDetailLabel("Rent Price/Day: ", currency.format(car.getPricePerDay())));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private Label createDetailLabel(String title, String value) {
        Label lbl = new Label(title + (value == null ? "N/A" : value));
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        return lbl;
    }

    private void showCarImagesWindow(Car car) {
        if (car.getImages() == null || car.getImages().isEmpty()) {
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Gallery: " + car.getBrand() + " " + car.getModel());

        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #1E1E1E;");

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(layout.widthProperty().subtract(150));
        imageView.fitHeightProperty().bind(layout.heightProperty().subtract(100));

        StackPane imagePane = new StackPane(imageView);
        imagePane.setAlignment(Pos.CENTER);
        layout.setCenter(imagePane);

        Button btnPrev = new Button("❮");
        Button btnNext = new Button("❯");
        String btnStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 10 20;";
        btnPrev.setStyle(btnStyle);
        btnNext.setStyle(btnStyle);

        VBox leftBox = new VBox(btnPrev);
        leftBox.setAlignment(Pos.CENTER);
        leftBox.setPadding(new Insets(0, 10, 0, 10));
        VBox rightBox = new VBox(btnNext);
        rightBox.setAlignment(Pos.CENTER);
        rightBox.setPadding(new Insets(0, 10, 0, 10));
        layout.setLeft(leftBox);
        layout.setRight(rightBox);

        Label lblCount = new Label();
        lblCount.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
        BorderPane.setAlignment(lblCount, Pos.CENTER);
        layout.setBottom(lblCount);

        AtomicInteger currentIndex = new AtomicInteger(0);
        List<String> images = car.getImages();

        Runnable updateView = () -> {
            int index = currentIndex.get();
            btnPrev.setDisable(index == 0);
            btnNext.setDisable(index == images.size() - 1);
            btnPrev.setOpacity(index == 0 ? 0.3 : 1.0);
            btnNext.setOpacity(index == images.size() - 1 ? 0.3 : 1.0);
            lblCount.setText((index + 1) + " / " + images.size());
            try {
                File file = new File(images.get(index));
                if (file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                } else {
                    imageView.setImage(null);
                }
            } catch (Exception e) {
                imageView.setImage(null);
            }
        };

        btnPrev.setOnAction(e -> {
            if (currentIndex.get() > 0) {
                currentIndex.decrementAndGet();
                updateView.run();
            }
        });
        btnNext.setOnAction(e -> {
            if (currentIndex.get() < images.size() - 1) {
                currentIndex.incrementAndGet();
                updateView.run();
            }
        });

        updateView.run();

        Scene scene = new Scene(layout, 1000, 700);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT && currentIndex.get() > 0) {
                btnPrev.fire();
            } else if (event.getCode() == KeyCode.RIGHT && currentIndex.get() < images.size() - 1) {
                btnNext.fire();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
        layout.requestFocus();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}