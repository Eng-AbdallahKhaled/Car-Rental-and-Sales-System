package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.App;
import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.util.InvoiceGenerator;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class CustomerPortalController {

    @FXML private VBox rootPane;
    @FXML private TextField txtSearch;
    @FXML private ImageView imgCarPreview;

    @FXML private Button btnShowImages;
    @FXML private Button btnGuestRent;
    @FXML private Button btnGuestBuy;
    @FXML private Button btnLogout;
    @FXML private Button btnMyTransactions; 

    // ✅ عناصر قسم الملف الشخصي المضافة حديثاً
    @FXML private ImageView imgCustomerProfile;
    @FXML private Label lblCustomerName;
    @FXML private ComboBox<String> comboCustomerGender;

    @FXML private TableView<Car> tableCars;
    @FXML private TableColumn<Car, String> colBrand;
    @FXML private TableColumn<Car, String> colModel;
    @FXML private TableColumn<Car, Integer> colYear;
    @FXML private TableColumn<Car, String> colColor;
    @FXML private TableColumn<Car, Double> colPricePerDay;
    @FXML private TableColumn<Car, Double> colSalePrice;
    @FXML private TableColumn<Car, String> colStatus;

    private final CarService carService;
    private final RentalService rentalService;
    private final SaleService saleService;
    private final CustomerService customerService;

    private ObservableList<Car> carList;
    private Customer loggedInCustomer;
    private String currentCustomerImagePath = null;

    public CustomerPortalController() {
        this.carService = new CarService();
        this.rentalService = new RentalService();
        this.saleService = new SaleService();
        this.customerService = new CustomerService();
    }

    public void setCustomer(Customer customer) {
        this.loggedInCustomer = customer;
        if (btnMyTransactions != null) {
            btnMyTransactions.setDisable(!hasTransactions(customer));
        }
        
        // ✅ تعبئة بيانات الملف الشخصي عند الدخول
        if (lblCustomerName != null) {
            lblCustomerName.setText(customer.getName());
        }
        if (comboCustomerGender != null) {
            comboCustomerGender.setValue(customer.getGender() != null ? customer.getGender() : "Not Specified");
        }
        currentCustomerImagePath = customer.getImagePath();
        loadCustomerImage(currentCustomerImagePath);
    }

    // ✅ دالة تحميل صورة العميل أو الصورة الافتراضية
    private void loadCustomerImage(String path) {
        if (imgCustomerProfile == null) return;
        
        if (path != null && !path.isEmpty()) {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                imgCustomerProfile.setImage(new Image(imgFile.toURI().toString(), true));
                return;
            }
        }
        // تحميل الصورة الافتراضية في حال عدم وجود مسار أو مسار مكسور
        try {
            Image defaultImg = new Image(App.class.getResourceAsStream("default_user.jpeg"));
            imgCustomerProfile.setImage(defaultImg);
        } catch (Exception e) {
            imgCustomerProfile.setImage(null);
        }
    }

    private boolean hasTransactions(Customer c) {
        if (c == null) {
            return false;
        }
        return rentalService.getAllRentals().stream().anyMatch(r -> r.getCustomer().getNationalId() != null && r.getCustomer().getNationalId().equals(c.getNationalId()))
                || saleService.getAllSales().stream().anyMatch(s -> s.getCustomer().getNationalId() != null && s.getCustomer().getNationalId().equals(c.getNationalId()));
    }

    @FXML
    public void initialize() {
        setupTableColumns();

        tableCars.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        loadCarsWithSearch();
        
        // إعداد خيارات النوع
        if (comboCustomerGender != null) {
            comboCustomerGender.getItems().addAll("Male", "Female", "Not Specified");
        }

        if (btnShowImages != null) {
            btnShowImages.setDisable(true);
        }
        if (btnGuestRent != null) {
            btnGuestRent.setDisable(true);
        }
        if (btnGuestBuy != null) {
            btnGuestBuy.setDisable(true);
        }

        tableCars.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateImagePreview(newSelection);

                boolean hasImages = newSelection.getImages() != null && !newSelection.getImages().isEmpty();
                if (btnShowImages != null) {
                    btnShowImages.setDisable(!hasImages);
                }

                boolean isFreeToday = isCarFreeToday(newSelection);
                LocalDate nextBooking = getNextBookingStart(newSelection);

                if (btnGuestRent != null) {
                    btnGuestRent.setDisable(!isFreeToday);
                }

                if (btnGuestBuy != null) {
                    btnGuestBuy.setDisable(!isFreeToday || nextBooking != null);
                }
            } else {
                imgCarPreview.setImage(null);
                if (btnShowImages != null) {
                    btnShowImages.setDisable(true);
                }
                if (btnGuestRent != null) {
                    btnGuestRent.setDisable(true);
                }
                if (btnGuestBuy != null) {
                    btnGuestBuy.setDisable(true);
                }
            }
        });

        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> rootPane.requestFocus());
        }
    }

    // ✅ دالة رفع صورة الملف الشخصي من قِبل العميل
    @FXML
    void btnUploadProfileImgAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            File destDir = new File("customer_images");
            if (!destDir.exists()) destDir.mkdirs();

            try {
                String newFileName = System.currentTimeMillis() + "_" + file.getName();
                File destFile = new File(destDir, newFileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                currentCustomerImagePath = "customer_images/" + newFileName;
                loadCustomerImage(currentCustomerImagePath); // تحديث الصورة في الواجهة فوراً

            } catch (IOException e) {
                showAlert("Error", "Failed to upload image: " + e.getMessage());
            }
        }
    }

    // ✅ دالة حفظ تعديلات الملف الشخصي
    @FXML
    void btnSaveProfileAction(ActionEvent event) {
        if (loggedInCustomer != null) {
            loggedInCustomer.setGender(comboCustomerGender.getValue());
            loggedInCustomer.setImagePath(currentCustomerImagePath);
            
            customerService.updateCustomer(loggedInCustomer);
            showAlert("Success", "Profile updated successfully!");
        }
    }

    private boolean isCarFreeToday(Car car) {
        boolean isSold = saleService.getAllSales().stream().anyMatch(s -> s.getCar().getId().equals(car.getId()));
        if (isSold) {
            return false;
        }

        boolean isRentedNow = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .anyMatch(r -> !LocalDate.now().isBefore(r.getStartDate()) && !LocalDate.now().isAfter(r.getEndDate()));

        return !isRentedNow;
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

    @FXML
    void btnMyTransactionsAction(ActionEvent event) {
        if (loggedInCustomer != null) {
            showTransactionsDialog(loggedInCustomer);
        } else {
            showAlert("Error", "No customer logged in.");
        }
    }

    public void showTransactionsDialog(Customer customer) {
        Stage stage = new Stage();
        stage.setTitle("My Transactions History");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label lblTitle = new Label("History for " + customer.getName());
        lblTitle.setStyle("-fx-text-fill: #fcd513; -fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<TransactionRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TransactionRow, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());

        TableColumn<TransactionRow, LocalDate> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());

        TableColumn<TransactionRow, String> colCar = new TableColumn<>("Car");
        colCar.setCellValueFactory(cell -> cell.getValue().carInfoProperty());

        TableColumn<TransactionRow, Double> colPrice = new TableColumn<>("Amount");
        colPrice.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());

        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(NumberFormat.getCurrencyInstance(Locale.US).format(price));
                }
            }
        });

        table.getColumns().addAll(colType, colDate, colCar, colPrice);

        Runnable refreshTable = () -> {
            List<TransactionRow> rows = new ArrayList<>();
            rentalService.getAllRentals().stream().filter(r -> r.getCustomer().getNationalId() != null && r.getCustomer().getNationalId().equals(customer.getNationalId()))
                    .forEach(r -> rows.add(new TransactionRow("Rental", r.getContractDate(), r.getRentalId(), r.getCar().getBrand() + " " + r.getCar().getModel(), r.getTotalPrice(), "Rentals", r)));

            saleService.getAllSales().stream().filter(s -> s.getCustomer().getNationalId() != null && s.getCustomer().getNationalId().equals(customer.getNationalId()))
                    .forEach(s -> rows.add(new TransactionRow("Sale", s.getSaleDate(), s.getSaleId(), s.getCar().getBrand() + " " + s.getCar().getModel(), s.getPrice(), "Sales", s)));

            rows.sort(Comparator.comparing(TransactionRow::getDate).reversed());
            table.setItems(FXCollections.observableArrayList(rows));
        };
        refreshTable.run();

        Button btnOpenPdf = new Button("View Contract");
        btnOpenPdf.setDisable(true);
        btnOpenPdf.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnCancel = new Button("Action");
        btnCancel.setDisable(true);
        btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                btnOpenPdf.setDisable(findPdf(newVal) == null);
                btnCancel.setDisable(false);
                btnCancel.setText(newVal.getType().equals("Sale") ? "Refund" : "Return & Calculate");
            } else {
                btnOpenPdf.setDisable(true);
                btnCancel.setDisable(true);
            }
        });

        btnOpenPdf.setOnAction(e -> {
            File f = findPdf(table.getSelectionModel().getSelectedItem());
            if (f != null) {
                try {
                    Desktop.getDesktop().open(f);
                } catch (Exception ex) {
                }
            }
        });

        btnCancel.setOnAction(e -> {
            TransactionRow sel = table.getSelectionModel().getSelectedItem();
            if (sel.getObj() instanceof Rental) {
                Rental r = (Rental) sel.getObj();
                double refund = rentalService.returnCar(r);
                InvoiceGenerator.generateReturnReceipt(sel.getId(), ((Rental) sel.getObj()).getCar(), customer);
                String msg = (refund > 0) ? String.format("Car Returned. Refund Amount: $%.2f (Unused Days)", refund) : "Car Returned. No Refund (Contract Ended).";
                showAlert("Success", msg);
            } else {
                saleService.returnCar((Sale) sel.getObj());
                InvoiceGenerator.generateSaleRefundReceipt(sel.getId(), ((Sale) sel.getObj()).getCar(), customer, sel.getPrice());
                showAlert("Success", "Transaction cancelled successfully.");
            }
            refreshTable.run();
            loadCarsWithSearch();
            if (btnMyTransactions != null) {
                btnMyTransactions.setDisable(!hasTransactions(loggedInCustomer));
            }
        });

        HBox footer = new HBox(15, btnCancel, btnOpenPdf);
        footer.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().addAll(lblTitle, table, footer);

        Scene scene = new Scene(layout, 850, 500);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private File findPdf(TransactionRow row) {
        File dir = new File("invoices/" + row.getFolder());
        if (!dir.exists() || dir.listFiles() == null) {
            return null;
        }
        for (File f : dir.listFiles()) {
            if (f.getName().contains(row.getId())) {
                return f;
            }
        }
        return null;
    }

    @FXML
    void btnShowImagesAction(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car != null && car.getImages() != null && !car.getImages().isEmpty()) {
            showCarImagesWindow(car);
        }
    }

    private void showCarImagesWindow(Car car) {
        if (car.getImages().isEmpty()) {
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
        String btnStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 28px; -fx-cursor: hand; -fx-padding: 20 25; -fx-background-radius: 10;";
        btnPrev.setStyle(btnStyle);
        btnNext.setStyle(btnStyle);

        VBox leftBox = new VBox(btnPrev);
        leftBox.setAlignment(Pos.CENTER);
        leftBox.setPadding(new Insets(0, 10, 0, 10));
        leftBox.prefHeightProperty().bind(layout.heightProperty());

        VBox rightBox = new VBox(btnNext);
        rightBox.setAlignment(Pos.CENTER);
        rightBox.setPadding(new Insets(0, 10, 0, 10));
        rightBox.prefHeightProperty().bind(layout.heightProperty());

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

    @FXML
    void btnLogoutAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("login_view.fxml"));
            Parent view = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            Scene scene = new Scene(view, stage.getWidth(), stage.getHeight());
            try {
                scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
            } catch (Exception e) {
            }
            stage.setScene(scene);
            if (wasMaximized) {
                stage.setMaximized(true);
            }
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnGuestRentAction(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car == null) {
            return;
        }
        showGuestDialog(car, true);
    }

    @FXML
    void btnGuestBuyAction(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car == null) {
            return;
        }
        LocalDate nextBooking = getNextBookingStart(car);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if (nextBooking != null) {
            showAlert("Error", "Car is reserved starting from " + nextBooking.format(fmt) + ". Buying now is disabled.");
            return;
        }
        showGuestDialog(car, false);
    }

    private void showGuestDialog(Car car, boolean isRent) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(isRent ? "Confirm Rental" : "Confirm Purchase");
        dialog.setHeaderText("Confirmation for: " + car.getBrand() + " " + car.getModel());

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Customer:"), 0, 0);
        Label lblName = new Label(loggedInCustomer.getName());
        lblName.setStyle("-fx-font-weight: bold;");
        grid.add(lblName, 1, 0);

        grid.add(new Label("Phone:"), 0, 1);
        grid.add(new Label(loggedInCustomer.getPhone()), 1, 1);

        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(1));

        List<Rental> carRentals = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar().getId().equals(car.getId()))
                .collect(Collectors.toList());

        Callback<DatePicker, DateCell> startDayCellFactory = picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    return;
                }

                Optional<Rental> conflict = carRentals.stream()
                        .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                        .findFirst();
                if (conflict.isPresent()) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    setTooltip(new Tooltip("Conflicts with existing booking: "
                            + conflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                            + " — " + conflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                    return;
                }

                Optional<Rental> nextFuture = carRentals.stream()
                        .filter(r -> r.getStartDate().isAfter(LocalDate.now()))
                        .min(Comparator.comparing(Rental::getStartDate));
                if (nextFuture.isPresent()) {
                    LocalDate nb = nextFuture.get().getStartDate();
                    if (!date.isBefore(nb) || date.isAfter(nb.minusDays(1))) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Reserved from " + nb.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                    }
                }
            }
        };
        startDate.setDayCellFactory(startDayCellFactory);

        Callback<DatePicker, DateCell> endDayCellFactory = picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate s = startDate.getValue();

                if (date.isBefore(LocalDate.now()) || (s != null && date.isBefore(s))) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                    return;
                }

                if (s != null) {
                    Optional<Rental> conflict = carRentals.stream()
                            .filter(r -> {
                                return !(date.isBefore(r.getStartDate()) || s.isAfter(r.getEndDate()));
                            })
                            .findFirst();
                    if (conflict.isPresent()) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Overlaps booking: "
                                + conflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                                + " — " + conflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                        return;
                    }
                } else {
                    Optional<Rental> conflict = carRentals.stream()
                            .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                            .findFirst();
                    if (conflict.isPresent()) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Conflicts with existing booking: "
                                + conflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                                + " — " + conflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                        return;
                    }
                }

                Optional<Rental> nextFuture = carRentals.stream()
                        .filter(r -> r.getStartDate().isAfter(LocalDate.now()))
                        .min(Comparator.comparing(Rental::getStartDate));
                if (nextFuture.isPresent() && s != null && s.isBefore(nextFuture.get().getStartDate())) {
                    LocalDate nb = nextFuture.get().getStartDate();
                    if (date.isAfter(nb.minusDays(1))) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffc0cb;");
                        setTooltip(new Tooltip("Reserved from " + nb.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                    }
                }
            }
        };
        endDate.setDayCellFactory(endDayCellFactory);

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        if (isRent) {
            grid.add(new Label("Start Date:"), 0, 2);
            grid.add(startDate, 1, 2);
            grid.add(new Label("End Date:"), 0, 3);
            grid.add(endDate, 1, 3);

            Label lblPrice = new Label("Price/Day: " + currency.format(car.getPricePerDay()));
            lblPrice.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            grid.add(lblPrice, 0, 4, 2, 1);

            Label lblTotal = new Label("Calculated upon dates");
            lblTotal.setStyle("-fx-text-fill: #0d47a1; -fx-font-weight: bold; -fx-font-size: 14px;");
            Label lblTotalText = new Label("Total Cost:");
            lblTotalText.setStyle("-fx-text-fill: #0d47a1; -fx-font-weight: bold;");
            grid.add(lblTotalText, 0, 5);
            grid.add(lblTotal, 1, 5);

            Runnable updatePrice = () -> {
                LocalDate s = startDate.getValue();
                LocalDate e = endDate.getValue();

                endDate.setDayCellFactory(picker -> {
                    return new DateCell() {
                        @Override
                        public void updateItem(LocalDate date, boolean empty) {
                            super.updateItem(date, empty);
                            if (date.isBefore(LocalDate.now()) || (s != null && date.isBefore(s))) {
                                setDisable(true);
                                setStyle("-fx-background-color: #ffc0cb;");
                                return;
                            }
                            if (s != null) {
                                Optional<Rental> conflict = carRentals.stream()
                                        .filter(r -> !(date.isBefore(r.getStartDate()) || s.isAfter(r.getEndDate())))
                                        .findFirst();
                                if (conflict.isPresent()) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #ffc0cb;");
                                    setTooltip(new Tooltip("Overlaps booking: " + conflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + " — " + conflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                                    return;
                                }
                                Optional<Rental> nextFuture = carRentals.stream()
                                        .filter(r -> r.getStartDate().isAfter(LocalDate.now()))
                                        .min(Comparator.comparing(Rental::getStartDate));
                                if (nextFuture.isPresent() && s.isBefore(nextFuture.get().getStartDate())) {
                                    LocalDate nb = nextFuture.get().getStartDate();
                                    if (date.isAfter(nb.minusDays(1))) {
                                        setDisable(true);
                                        setStyle("-fx-background-color: #ffc0cb;");
                                        setTooltip(new Tooltip("Reserved from " + nb.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                                    }
                                }
                            } else {
                                Optional<Rental> conflict = carRentals.stream()
                                        .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                                        .findFirst();
                                if (conflict.isPresent()) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #ffc0cb;");
                                    setTooltip(new Tooltip("Conflicts with existing booking: " + conflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + " — " + conflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
                                    return;
                                }
                            }
                        }
                    };
                });

                if (s != null && e != null && !e.isBefore(s)) {
                    long days = ChronoUnit.DAYS.between(s, e);
                    if (days == 0) {
                        days = 1;
                    }
                    lblTotal.setText(currency.format(days * car.getPricePerDay()));
                } else {
                    lblTotal.setText("...");
                }
            };
            startDate.valueProperty().addListener((o, ol, n) -> updatePrice.run());
            endDate.valueProperty().addListener((o, ol, n) -> updatePrice.run());

            updatePrice.run();

        } else {
            grid.add(new Label("Total Price:"), 0, 2);
            Label lblPrice = new Label(currency.format(car.getSalePrice()));
            lblPrice.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; font-size: 14px;");
            grid.add(lblPrice, 1, 2);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> dialogButton == confirmButtonType);

        Optional<Boolean> result = dialog.showAndWait();

        if (result.isPresent() && result.get()) {
            try {
                if (isRent) {
                    if (endDate.getValue().isBefore(startDate.getValue())) {
                        showAlert("Error", "End date cannot be before start date!");
                        return;
                    }
                    if (startDate.getValue().isBefore(LocalDate.now())) {
                        showAlert("Error", "Start date cannot be in the past!");
                        return;
                    }

                    LocalDate nextBookingCheck = getNextBookingStart(car);
                    if (nextBookingCheck != null && endDate.getValue().isAfter(nextBookingCheck.minusDays(1))) {
                        showAlert("Error", "Car is reserved starting from " + nextBookingCheck + ". Please select an earlier end date.");
                        return;
                    }

                    LocalDate s = startDate.getValue();
                    LocalDate e = endDate.getValue();
                    Optional<Rental> finalConflict = rentalService.getAllRentals().stream()
                            .filter(r -> r.getCar().getId().equals(car.getId()))
                            .filter(r -> !(e.isBefore(r.getStartDate()) || s.isAfter(r.getEndDate())))
                            .findFirst();
                    if (finalConflict.isPresent()) {
                        showAlert("Error", "Selected period overlaps existing booking ("
                                + finalConflict.get().getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                                + " - " + finalConflict.get().getEndDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ").");
                        return;
                    }

                    long days = ChronoUnit.DAYS.between(startDate.getValue(), endDate.getValue());
                    if (days == 0) {
                        days = 1;
                    }
                    double totalPrice = days * car.getPricePerDay();

                    rentalService.rentCar(car, loggedInCustomer, startDate.getValue(), endDate.getValue());
                    showAlert("Success", "Car Rented Successfully! Please pick up your car.");

                    List<Rental> list = rentalService.getAllRentals();
                    String id = list.get(list.size() - 1).getRentalId();
                    InvoiceGenerator.generateRentalInvoice(id, car, loggedInCustomer, totalPrice, startDate.getValue(), endDate.getValue());

                    if (btnMyTransactions != null) {
                        btnMyTransactions.setDisable(!hasTransactions(loggedInCustomer));
                    }

                } else {
                    LocalDate nextBookingForBuy = getNextBookingStart(car);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    if (nextBookingForBuy != null) {
                        showAlert("Error", "Car is reserved starting from " + nextBookingForBuy.format(fmt) + ". Buying now is disabled.");
                        return;
                    }

                    saleService.sellCar(car, loggedInCustomer, car.getSalePrice());
                    showAlert("Success", "Car Purchased Successfully! Congratulations.");

                    List<Sale> list = saleService.getAllSales();
                    String id = list.get(list.size() - 1).getSaleId();
                    InvoiceGenerator.generateSaleInvoice(id, car, loggedInCustomer, car.getSalePrice());

                    if (btnMyTransactions != null) {
                        btnMyTransactions.setDisable(!hasTransactions(loggedInCustomer));
                    }
                }
                loadCarsWithSearch();
            } catch (Exception e) {
                showAlert("Error", e.getMessage());
            }
        }
    }

    private void setupTableColumns() {
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colPricePerDay.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        colSalePrice.setCellValueFactory(new PropertyValueFactory<>("salePrice"));

        colStatus.setCellValueFactory(cellData -> {
            Car car = cellData.getValue();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            if (car.isAvailable()) {
                LocalDate nextBooking = getNextBookingStart(car);
                if (nextBooking != null) {
                    return new SimpleStringProperty("⚠️ Available until: " + nextBooking.format(fmt));
                } else {
                    return new SimpleStringProperty("✅ Available Now");
                }
            } else {
                boolean isSold = saleService.getAllSales().stream()
                        .anyMatch(s -> s.getCar().getId().equals(car.getId()));
                if (isSold) {
                    return new SimpleStringProperty("❌ Sold Out");
                }

                Optional<Rental> activeRental = rentalService.getAllRentals().stream()
                        .filter(r -> r.getCar().getId().equals(car.getId()))
                        .filter(r -> !r.getEndDate().isBefore(LocalDate.now()))
                        .sorted((r1, r2) -> r1.getStartDate().compareTo(r2.getStartDate()))
                        .findFirst();

                if (activeRental.isPresent()) {
                    LocalDate start = activeRental.get().getStartDate();
                    if (start.isAfter(LocalDate.now())) {
                        return new SimpleStringProperty("⚠️ Available until: " + start.format(fmt));
                    } else {
                        return new SimpleStringProperty("🚫 Rented until: " + activeRental.get().getEndDate().format(fmt));
                    }
                }
                return new SimpleStringProperty("❌ Not Available");
            }
        });

        colStatus.setCellFactory(tc -> new TableCell<Car, String>() {
            @Override
            protected void updateItem(String statusText, boolean empty) {
                super.updateItem(statusText, empty);
                if (empty || statusText == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label lbl = new Label(statusText);
                lbl.setStyle("-fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 6; -fx-border-radius: 6;");

                if (statusText.startsWith("✅")) {
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #27ae60; -fx-text-fill: white;");
                } else if (statusText.startsWith("⚠️")) {
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #f1c40f; -fx-text-fill: #2c3e50;");
                } else if (statusText.startsWith("🚫")) {
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #8e44ad; -fx-text-fill: white;");
                } else if (statusText.startsWith("❌")) {
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #c0392b; -fx-text-fill: white;");
                } else {
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #fcd513;");
                }

                setText(null);
                setGraphic(lbl);
            }
        });

        formatCurrencyColumn(colPricePerDay);
        formatCurrencyColumn(colSalePrice);
    }

    private void loadCarsWithSearch() {
        carList = FXCollections.observableArrayList(carService.getAllCars());
        FilteredList<Car> filteredData = new FilteredList<>(carList, b -> true);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(car -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerFilter = newValue.toLowerCase();
                    String carFullData = (car.getBrand() + " " + car.getModel() + " " + car.getYear() + " " + car.getColor()).toLowerCase();
                    String[] words = carFullData.split(" ");
                    for (String word : words) {
                        if (word.startsWith(lowerFilter)) {
                            return true;
                        }
                    }
                    if (carFullData.startsWith(lowerFilter)) {
                        return true;
                    }
                    return false;
                });
            });
        }

        SortedList<Car> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCars.comparatorProperty());
        tableCars.setItems(sortedData);
    }

    private void updateImagePreview(Car car) {
        if (car.getImages() != null && !car.getImages().isEmpty()) {
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

    private void formatCurrencyColumn(TableColumn<Car, Double> column) {
        column.setCellFactory(tc -> new TableCell<Car, Double>() {
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class TransactionRow {

        private final SimpleStringProperty type;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty id;
        private final SimpleStringProperty carInfo;
        private final SimpleDoubleProperty price;
        private final SimpleStringProperty folder;
        private final Object obj;

        public TransactionRow(String t, LocalDate d, String i, String c, Double p, String f, Object o) {
            this.type = new SimpleStringProperty(t);
            this.date = new SimpleObjectProperty<>(d);
            this.id = new SimpleStringProperty(i);
            this.carInfo = new SimpleStringProperty(c);
            this.price = new SimpleDoubleProperty(p);
            this.folder = new SimpleStringProperty(f);
            this.obj = o;
        }

        public String getType() {
            return type.get();
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public LocalDate getDate() {
            return date.get();
        }

        public SimpleObjectProperty<LocalDate> dateProperty() {
            return date;
        }

        public String getId() {
            return id.get();
        }

        public String getCarInfo() {
            return carInfo.get();
        }

        public SimpleStringProperty carInfoProperty() {
            return carInfo;
        }

        public Double getPrice() {
            return price.get();
        }

        public SimpleDoubleProperty priceProperty() {
            return price;
        }

        public String getFolder() {
            return folder.get();
        }

        public Object getObj() {
            return obj;
        }
    }
}