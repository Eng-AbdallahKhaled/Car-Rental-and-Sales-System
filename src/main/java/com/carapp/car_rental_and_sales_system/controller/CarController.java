package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.util.InvoiceGenerator;
import com.carapp.car_rental_and_sales_system.util.ExportUtil;

import java.io.File;
import java.io.IOException; // تم الإضافة لمعالجة أخطاء النسخ
import java.nio.file.Files; // تم الإضافة لنسخ الصور
import java.nio.file.StandardCopyOption; // تم الإضافة لنسخ الصور
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.Parent;

public class CarController {

    @FXML
    private VBox rootPane;
    @FXML
    private TextField txtSearch, txtBrand, txtModel, txtYear, txtPricePerDay, txtSalePrice, txtColor;
    @FXML
    private ImageView imgCarPreview;

    @FXML
    private Button btnShowImages, btnEditGallery, btnAddCar, btnUpdateCar, btnDeleteCar, btnUploadImage;

    @FXML
    private TableView<Car> tableCars;
    @FXML
    private TableColumn<Car, String> colId, colBrand, colModel;
    @FXML
    private TableColumn<Car, Integer> colYear;
    @FXML
    private TableColumn<Car, String> colColor;
    @FXML
    private TableColumn<Car, Double> colPricePerDay, colSalePrice;
    @FXML
    private TableColumn<Car, String> colAvailable;

    // DETAILS PANE controls
    @FXML
    private VBox detailsPane;
    @FXML
    private Label detailsTitleLbl;
    @FXML
    private Label detailsStatusLbl;
    @FXML
    private Label detailsHolderLbl;
    @FXML
    private Label detailsPhoneLbl;
    @FXML
    private Label detailsContractLbl;
    @FXML
    private VBox detailsEntriesBox;
    @FXML
    private Button detailsOpenPdfBtn;
    @FXML
    private Button detailsRefundBtn;

    private final CarService carService;
    private final SaleService saleService;
    private final RentalService rentalService;

    private ObservableList<Car> carList;
    private List<String> selectedImagePaths = new ArrayList<>();

    private String currentUserRole = "Authorized";
    private final SimpleBooleanProperty isAuthorized = new SimpleBooleanProperty(true);

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // caches
    private final Map<String, Rental> currentRentalByCarId = new HashMap<>();
    private final Map<String, Rental> nextRentalByCarId = new HashMap<>();
    private final Map<String, Sale> saleByCarId = new HashMap<>();

    // Logic Flags
    private String detailsCarIdDisplayed = null;
    // Flag to protect pane from closing when clicking the label (Smart Switching)
    private volatile boolean isLabelClickInteraction = false;

    // Theme Colors
    private static final String COLOR_GOLD = "#fcd513";
    private static final String COLOR_WHEAT_LABEL = "#F5DEB3";
    private static final String COLOR_WHITE_VALUE = "#FFFFFF";
    private static final String COLOR_SILVER_STATUS = "#C0C0C0";
    private static final String COLOR_GREEN_PRICE = "#2ecc71";

    public CarController() {
        this.carService = new CarService();
        this.saleService = new SaleService();
        this.rentalService = new RentalService();
    }

    public void setSession(String role) {
        this.currentUserRole = role;
        isAuthorized.set(!"Admin".equals(role));
        applyPermissions();
    }

    private void applyPermissions() {
        if ("Admin".equals(currentUserRole)) {
            if (btnUpdateCar != null) {
                btnUpdateCar.setVisible(false);
                btnUpdateCar.setManaged(false);
            }
            if (btnDeleteCar != null) {
                btnDeleteCar.setVisible(false);
                btnDeleteCar.setManaged(false);
            }
            if (btnEditGallery != null) {
                btnEditGallery.setVisible(false);
                btnEditGallery.setManaged(false);
            }
        }
    }

    @FXML
    public void initialize() {
        // --- 1. LAYOUT FIX: Remove gaps & Ensure ScrollPane fills the void ---
        if (detailsEntriesBox != null) {
            Parent parent = detailsEntriesBox.getParent();
            if (parent instanceof ScrollPane) {
                ScrollPane sp = (ScrollPane) parent;
                VBox.setVgrow(sp, Priority.ALWAYS); // CRITICAL: This fills the black void
                sp.setFitToWidth(true);
                sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                VBox.setMargin(sp, new Insets(0)); // Remove ALL margins
                sp.setPadding(new Insets(0));      // Remove ALL padding
            }
            detailsEntriesBox.setSpacing(6);
            detailsEntriesBox.setPadding(new Insets(0)); // Remove padding
        }

        // Table columns binding
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colPricePerDay.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        colSalePrice.setCellValueFactory(new PropertyValueFactory<>("salePrice"));

        // Availability Logic
        colAvailable.setCellValueFactory(cellData -> {
            Car car = cellData.getValue();
            LocalDate today = LocalDate.now();

            boolean isSold = saleService.getAllSales().stream()
                    .anyMatch(s -> s.getCar().getId().equals(car.getId()));
            if (isSold) {
                return new SimpleStringProperty("SOLD");
            }

            Optional<Rental> currentRental = rentalService.getAllRentals().stream()
                    .filter(r -> r.getCar().getId().equals(car.getId()))
                    .filter(r -> !today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate()))
                    .findFirst();
            if (currentRental.isPresent()) {
                return new SimpleStringProperty("RENTED:" + currentRental.get().getEndDate().format(dateFmt));
            }

            Optional<Rental> futureRental = rentalService.getAllRentals().stream()
                    .filter(r -> r.getCar().getId().equals(car.getId()))
                    .filter(r -> r.getStartDate().isAfter(today))
                    .min(Comparator.comparing(Rental::getStartDate));
            if (futureRental.isPresent()) {
                LocalDate until = futureRental.get().getStartDate().minusDays(1);
                return new SimpleStringProperty("AVAILABLE_UNTIL:" + until.format(dateFmt));
            }
            return new SimpleStringProperty("AVAILABLE_NOW");
        });

        // Custom Cell Factory with SMART INTERACTION Logic
        colAvailable.setCellFactory(col -> new TableCell<Car, String>() {
            @Override
            protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);
                if (empty || code == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label lbl = new Label();
                lbl.setStyle("-fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand;");

                // Colors
                if ("AVAILABLE_NOW".equals(code)) {
                    lbl.setText("✅ Available Now");
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #27ae60;");
                } else if (code.startsWith("AVAILABLE_UNTIL:")) {
                    String d = code.substring("AVAILABLE_UNTIL:".length());
                    lbl.setText("⚠️ Available until " + d);
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #f39c12; -fx-text-fill:black;");
                } else if (code.startsWith("RENTED:")) {
                    String d = code.substring("RENTED:".length());
                    lbl.setText("🚫 Rented until " + d);
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #8e44ad;");
                } else if ("SOLD".equals(code)) {
                    lbl.setText("❌ Sold Out");
                    lbl.setStyle(lbl.getStyle() + "-fx-background-color: #c0392b;");
                }

                // --- 1. SET FLAG ON MOUSE PRESS ---
                // This happens BEFORE the TableView selection listener fires.
                lbl.setOnMousePressed(e -> {
                    isLabelClickInteraction = true;
                });

                // --- 2. HANDLE CLICK LOGIC ---
                lbl.setOnMouseClicked(evt -> {
                    // RESET FLAG IMMEDIATELY to prevent "Two Movement" bug
                    isLabelClickInteraction = false;

                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        return;
                    }
                    Car clickedCar = getTableView().getItems().get(getIndex());
                    if (clickedCar == null) {
                        return;
                    }

                    if ("AVAILABLE_NOW".equals(code) || lbl.getText().toLowerCase().contains("available now")) {
                        return;
                    }

                    // A. TOGGLE LOGIC: If same car is already open -> Close.
                    if (detailsPane.isVisible() && detailsCarIdDisplayed != null && detailsCarIdDisplayed.equals(clickedCar.getId())) {
                        hideDetailsPaneAnimated();
                        detailsCarIdDisplayed = null;
                        return;
                    }

                    // B. SWITCHING LOGIC: Different car -> Update (Keep Open).
                    // Ensure the row is selected (in case clicks didn't trigger selection due to some consumption)
                    tableCars.getSelectionModel().select(clickedCar);

                    updateDetailsPaneForCar(clickedCar);

                    // Re-assert VGrow (just in case)
                    if (detailsEntriesBox != null && detailsEntriesBox.getParent() instanceof ScrollPane) {
                        VBox.setVgrow((ScrollPane) detailsEntriesBox.getParent(), Priority.ALWAYS);
                    }
                });

                setGraphic(lbl);
                setText(null);
            }
        });

        formatCurrencyColumn(colPricePerDay);
        formatCurrencyColumn(colSalePrice);
        tableCars.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        loadCarsWithSearch();
        buildCaches();

        // Buttons state
        if (btnShowImages != null) {
            btnShowImages.setDisable(true);
        }
        if (btnEditGallery != null) {
            btnEditGallery.setDisable(true);
        }
        if (btnUpdateCar != null) {
            btnUpdateCar.setDisable(true);
        }
        if (btnDeleteCar != null) {
            btnDeleteCar.setDisable(true);
        }
        if (btnEditGallery != null && btnShowImages != null) {
            btnEditGallery.disableProperty().bind(btnShowImages.disableProperty().or(isAuthorized.not()));
        }

        // --- 3. STRICT SELECTION LISTENER ---
        tableCars.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // CHECK FLAG: If this selection change was caused by the Label Click...
            if (isLabelClickInteraction) {
                isLabelClickInteraction = false; // Consume flag
                // Do NOT close. Do NOT toggle. Just let the Click Handler do its job (Update/Open).
                if (newSelection != null) {
                    fillFields(newSelection);
                }
                return;
            }

            // Normal Navigation (Keyboard Arrows / Row Background Click) -> ALWAYS CLOSE IMMEDIATELY
            if (newSelection != null) {
                fillFields(newSelection);
                boolean isAuth = !"Admin".equals(currentUserRole);
                if (btnUpdateCar != null && isAuth) {
                    btnUpdateCar.setDisable(false);
                }
                if (btnDeleteCar != null && isAuth) {
                    btnDeleteCar.setDisable(false);
                }
                if (btnShowImages != null) {
                    btnShowImages.setDisable(newSelection.getImages().isEmpty());
                }
            } else {
                clearFields();
                if (btnShowImages != null) {
                    btnShowImages.setDisable(true);
                }
                if (btnUpdateCar != null) {
                    btnUpdateCar.setDisable(true);
                }
                if (btnDeleteCar != null) {
                    btnDeleteCar.setDisable(true);
                }
            }

            // This is the "Close Immediately" requirement
            hideDetailsPaneAnimated();
            detailsCarIdDisplayed = null;
        });

        if (detailsStatusLbl != null) {
            detailsStatusLbl.setStyle("-fx-text-fill: " + COLOR_SILVER_STATUS + "; -fx-font-size: 13px;");
        }
        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> rootPane.requestFocus());
        }
    }

    private void buildCaches() {
        currentRentalByCarId.clear();
        nextRentalByCarId.clear();
        saleByCarId.clear();

        LocalDate today = LocalDate.now();

        for (Sale s : saleService.getAllSales()) {
            if (s != null && s.getCar() != null) {
                saleByCarId.put(s.getCar().getId(), s);
            }
        }

        for (Rental r : rentalService.getAllRentals()) {
            if (r == null || r.getCar() == null) {
                continue;
            }
            String id = r.getCar().getId();
            if (!today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate())) {
                currentRentalByCarId.putIfAbsent(id, r);
            }
            if (r.getStartDate().isAfter(today)) {
                Rental existing = nextRentalByCarId.get(id);
                if (existing == null || r.getStartDate().isBefore(existing.getStartDate())) {
                    nextRentalByCarId.put(id, r);
                }
            }
        }
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

        buildCaches();
    }

    // ✅ دالة تصدير الجدول إلى إكسيل
    @FXML
    void btnExportToExcelAction(ActionEvent event) {
        ExportUtil.exportTableToExcel(tableCars, rootPane.getScene().getWindow(), "Cars_Inventory");
    }

    @FXML
    void btnShowImagesAction(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car != null && car.getImages() != null && !car.getImages().isEmpty()) {
            showCarImagesWindow(car, false);
        }
    }

    @FXML
    void btnEditGalleryAction(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car != null) {
            showCarImagesWindow(car, true);
        }
    }

    private void showCarImagesWindow(Car car, boolean allowEdit) {
        if (allowEdit) {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Gallery");
            dialog.setHeaderText("Manage Images for: " + car.getBrand() + " " + car.getModel());
            HBox imageContainer = new HBox(15);
            imageContainer.setAlignment(Pos.CENTER_LEFT);
            imageContainer.setPadding(new Insets(10));
            refreshGalleryView(car, imageContainer);
            ScrollPane scrollPane = new ScrollPane(imageContainer);
            scrollPane.setFitToHeight(true);
            scrollPane.setPannable(true);
            scrollPane.setPrefSize(900, 420);
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
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

    private void refreshGalleryView(Car car, HBox container) {
        container.getChildren().clear();

        if (car.getImages() != null) {
            for (String path : car.getImages()) {
                VBox imageBox = new VBox(5);
                imageBox.setAlignment(Pos.CENTER);
                imageBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 5; -fx-background-color: white;");

                try {
                    File file = new File(path);
                    if (file.exists()) {
                        Image img = new Image(file.toURI().toString());
                        ImageView imageView = new ImageView(img);
                        imageView.setFitHeight(200);
                        imageView.setFitWidth(300);
                        imageView.setPreserveRatio(true);

                        imageBox.getChildren().add(imageView);

                        Button btnDeleteImg = new Button("❌ Delete");
                        btnDeleteImg.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                        btnDeleteImg.setPrefWidth(300);
                        btnDeleteImg.setOnAction(e -> {
                            car.getImages().remove(path);
                            carService.updateCar(car);
                            refreshGalleryView(car, container);

                            if (car.getImages().isEmpty()) {
                                imgCarPreview.setImage(null);
                                if (btnShowImages != null) {
                                    btnShowImages.setDisable(true);
                                }
                            } else if (!selectedImagePaths.isEmpty() && path.equals(selectedImagePaths.get(0))) {
                                fillFields(car);
                            }

                            buildCaches();
                        });
                        imageBox.getChildren().add(btnDeleteImg);
                        container.getChildren().add(imageBox);
                    }
                } catch (Exception e) {
                }
            }
        }

        VBox addBox = new VBox(5);
        addBox.setAlignment(Pos.CENTER);
        addBox.setPrefSize(200, 200);
        addBox.setStyle("-fx-border-color: #27ae60; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-color: rgba(39,174,96,0.08); -fx-cursor: hand;");

        Label lblPlus = new Label("+");
        lblPlus.setStyle("-fx-font-size: 50px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        Label lblText = new Label("Add Image");
        lblText.setStyle("-fx-font-size: 16px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        addBox.getChildren().addAll(lblPlus, lblText);
        addBox.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Add Images");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            List<File> files = fileChooser.showOpenMultipleDialog(null);

            if (files != null) {
                for (File f : files) {
                    car.getImages().add(f.getAbsolutePath());
                }
                carService.updateCar(car);
                fillFields(car);
                refreshGalleryView(car, container);

                if (btnShowImages != null) {
                    btnShowImages.setDisable(false);
                }

                buildCaches();
            }
        });

        container.getChildren().add(addBox);
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

    private void loadCars() {
        carList = FXCollections.observableArrayList(carService.getAllCars());
        tableCars.setItems(carList);
        buildCaches();
    }

    private void fillFields(Car car) {
        if (txtBrand != null) {
            txtBrand.setText(car.getBrand());
        }
        if (txtModel != null) {
            txtModel.setText(car.getModel());
        }
        if (txtYear != null) {
            txtYear.setText(String.valueOf(car.getYear()));
        }
        if (txtColor != null) {
            txtColor.setText(car.getColor());
        }
        if (txtPricePerDay != null) {
            txtPricePerDay.setText(String.valueOf(car.getPricePerDay()));
        }
        if (txtSalePrice != null) {
            txtSalePrice.setText(String.valueOf(car.getSalePrice()));
        }

        selectedImagePaths = new ArrayList<>(car.getImages());
        updateImagePreview();
    }

    @FXML
    void btnUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Car Images");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) txtBrand.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            File destDir = new File("car_images");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            for (File file : selectedFiles) {
                try {
                    String newFileName = System.currentTimeMillis() + "_" + file.getName();
                    File destFile = new File(destDir, newFileName);
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    selectedImagePaths.add("car_images/" + newFileName);
                } catch (IOException ex) {
                    System.out.println("Error copying image: " + ex.getMessage());
                }
            }
            updateImagePreview();
            showAlert("Success", selectedFiles.size() + " images added!");

            buildCaches();
        }
    }

    private void updateImagePreview() {
        if (selectedImagePaths != null && !selectedImagePaths.isEmpty()) {
            boolean found = false;
            for (String path : selectedImagePaths) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        imgCarPreview.setImage(new Image(file.toURI().toString()));
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                }
            }
            if (!found) {
                imgCarPreview.setImage(null);
            }
        } else {
            imgCarPreview.setImage(null);
        }
    }

    @FXML
    void btnAddCar(ActionEvent event) {
        try {
            String brand = txtBrand.getText();
            String model = txtModel.getText();
            int year = Integer.parseInt(txtYear.getText());
            double rentPrice = Double.parseDouble(txtPricePerDay.getText());
            double salePrice = Double.parseDouble(txtSalePrice.getText());
            String color = txtColor.getText();

            carService.addCar(brand, model, year, rentPrice, salePrice, color, selectedImagePaths);

            loadCarsWithSearch();
            clearFields();
            showAlert("Success", "Car added successfully!");

            buildCaches();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for Year and Prices.");
        } catch (Exception e) {
            showAlert("Error", "Something went wrong: " + e.getMessage());
        }
    }

    @FXML
    void btnUpdateCar(ActionEvent event) {
        Car selectedCar = tableCars.getSelectionModel().getSelectedItem();
        if (selectedCar == null) {
            showAlert("Warning", "Please select a car to update.");
            return;
        }

        try {
            selectedCar.setBrand(txtBrand.getText());
            selectedCar.setModel(txtModel.getText());
            selectedCar.setYear(Integer.parseInt(txtYear.getText()));
            selectedCar.setColor(txtColor.getText());
            selectedCar.setPricePerDay(Double.parseDouble(txtPricePerDay.getText()));
            selectedCar.setSalePrice(Double.parseDouble(txtSalePrice.getText()));

            selectedCar.setImages(new ArrayList<>(selectedImagePaths));

            carService.updateCar(selectedCar);
            tableCars.refresh();
            clearFields();
            showAlert("Success", "Car updated successfully!");

            buildCaches();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers.");
        }
    }

    @FXML
    void btnDeleteCar(ActionEvent event) {
        Car selectedCar = tableCars.getSelectionModel().getSelectedItem();
        if (selectedCar != null) {
            carService.deleteCar(selectedCar);
            loadCarsWithSearch();
            clearFields();
            buildCaches();
        } else {
            showAlert("Warning", "Please select a car to delete.");
        }
    }

    private void clearFields() {
        if (txtBrand != null) {
            txtBrand.clear();
        }
        if (txtModel != null) {
            txtModel.clear();
        }
        if (txtYear != null) {
            txtYear.setText("");
        }
        if (txtPricePerDay != null) {
            txtPricePerDay.clear();
        }
        if (txtSalePrice != null) {
            txtSalePrice.clear();
        }
        if (txtColor != null) {
            txtColor.clear();
        }
        if (imgCarPreview != null) {
            imgCarPreview.setImage(null);
        }
        selectedImagePaths = new ArrayList<>();
        if (tableCars != null) {
            tableCars.getSelectionModel().clearSelection();
        }

        hideDetailsPaneAnimated();
        detailsCarIdDisplayed = null;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // -------------------------
    // UTILS: Clipboard with Color Matching & Delay
    // -------------------------
    private void copyToClipboardAndToast(Node owner, String text) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);

            final Tooltip t = new Tooltip("Copied!");
            javafx.geometry.Point2D p = owner.localToScreen(owner.getBoundsInLocal().getMaxX(), owner.getBoundsInLocal().getMinY());
            if (p != null) {
                t.show(owner, p.getX(), p.getY());
            }
            PauseTransition pause = new PauseTransition(Duration.millis(900));
            pause.setOnFinished(ev -> t.hide());
            pause.play();
        } catch (Exception e) {
            // ignore errors
        }
    }

    private Button createCopyButtonForValue(String value, String colorHex, String tooltipText) {
        Button copy = new Button("📋");
        copy.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12px; -fx-text-fill: " + colorHex + ";");
        Tooltip tt = new Tooltip(tooltipText);
        tt.setShowDelay(Duration.millis(500)); // 0.5s Delay
        copy.setTooltip(tt);
        copy.setOnAction(ev -> copyToClipboardAndToast(copy, value));
        return copy;
    }

    // Helper for "Label: Value [Copy]"
    private HBox createLabeledValueWithCopy(String labelText, String valueText, String labelColorHex, String valueColorHex, String tooltipForValue) {
        Label lblLabel = new Label(labelText);
        lblLabel.setStyle("-fx-text-fill: " + labelColorHex + "; -fx-font-size: 12px;");

        Label lblValue = new Label(valueText);
        lblValue.setStyle("-fx-text-fill: " + valueColorHex + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button copy = createCopyButtonForValue(valueText, valueColorHex, tooltipForValue); // Icon matches Value Color

        HBox hb = new HBox(6, lblLabel, lblValue, copy);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    // -------------------------
    // DETAILS PANE LOGIC
    // -------------------------
    private void showDetailsPaneAnimated() {
        if (detailsPane == null) {
            return;
        }
        if (detailsPane.isVisible()) {
            return;
        }
        detailsPane.setTranslateX(220);
        detailsPane.setOpacity(0);
        detailsPane.setVisible(true);
        detailsPane.setManaged(true);

        TranslateTransition tt = new TranslateTransition(Duration.millis(260), detailsPane);
        tt.setFromX(220);
        tt.setToX(0);
        FadeTransition ft = new FadeTransition(Duration.millis(260), detailsPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        tt.play();
        ft.play();
    }

    private void hideDetailsPaneAnimated() {
        if (detailsPane == null) {
            return;
        }
        if (!detailsPane.isVisible()) {
            return;
        }

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), detailsPane);
        tt.setFromX(0);
        tt.setToX(220);
        FadeTransition ft = new FadeTransition(Duration.millis(220), detailsPane);
        ft.setFromValue(1);
        ft.setToValue(0);
        tt.setOnFinished(e -> {
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
            detailsPane.setTranslateX(0);
            detailsPane.setOpacity(1);
        });
        tt.play();
        ft.play();
    }

    private void updateDetailsPaneForCar(Car car) {
        if (detailsPane == null || car == null) {
            return;
        }

        buildCaches();
        detailsCarIdDisplayed = car.getId();

        detailsTitleLbl.setText(car.getBrand() + " " + car.getModel());
        detailsStatusLbl.setText("");
        detailsHolderLbl.setGraphic(null);
        detailsHolderLbl.setText("");
        detailsPhoneLbl.setGraphic(null);
        detailsPhoneLbl.setText("");
        detailsContractLbl.setGraphic(null);
        detailsContractLbl.setText("");

        if (detailsOpenPdfBtn != null) {
            detailsOpenPdfBtn.setVisible(false);
            detailsOpenPdfBtn.setManaged(false);
        }
        if (detailsRefundBtn != null) {
            detailsRefundBtn.setVisible(false);
            detailsRefundBtn.setManaged(false);
        }

        if (detailsEntriesBox != null) {
            detailsEntriesBox.getChildren().clear();
        }

        // 1. SOLD CAR LOGIC
        Sale sale = saleByCarId.get(car.getId());
        if (sale != null) {
            detailsStatusLbl.setText("Status: Sold");
            detailsStatusLbl.setStyle("-fx-text-fill: " + COLOR_SILVER_STATUS + "; -fx-font-size: 13px;");

            String buyerName = (sale.getCustomer() != null ? sale.getCustomer().getName() : "N/A");
            String buyerPhone = (sale.getCustomer() != null ? sale.getCustomer().getPhone() : "N/A");

            // Name & Phone ALWAYS Gold
            detailsHolderLbl.setGraphic(createLabeledValueWithCopy("Buyer:", buyerName, COLOR_WHEAT_LABEL, COLOR_GOLD, "Copy Name"));
            detailsPhoneLbl.setGraphic(createLabeledValueWithCopy("Phone:", buyerPhone, COLOR_WHEAT_LABEL, COLOR_GOLD, "Copy Phone"));

            // Invoice & Date (Wheat Label, White Value)
            String invoiceId = sale.getSaleId() != null ? sale.getSaleId() : "N/A";
            detailsContractLbl.setGraphic(createLabeledValueWithCopy("Invoice:", invoiceId, COLOR_WHEAT_LABEL, COLOR_WHITE_VALUE, "Copy Invoice ID"));

            String saleDate = (sale.getSaleDate() != null) ? sale.getSaleDate().format(dateFmt) : "N/A";
            HBox hbDate = createLabeledValueWithCopy("Date:", saleDate, COLOR_WHEAT_LABEL, COLOR_WHITE_VALUE, "Copy Sale Date");

            double salePrice = sale.getPrice();
            String salePriceFormatted = NumberFormat.getCurrencyInstance(Locale.US).format(salePrice);
            Label lblPrice = new Label("Sale Price: " + salePriceFormatted);
            lblPrice.setStyle("-fx-text-fill: " + COLOR_GREEN_PRICE + "; -fx-font-size: 13px; -fx-font-weight:bold;");
            HBox hbPrice = new HBox(8, lblPrice, createCopyButtonForValue(salePriceFormatted, COLOR_GREEN_PRICE, "Copy Sale Price"));
            hbPrice.setAlignment(Pos.CENTER_LEFT);

            if (detailsEntriesBox != null) {
                detailsEntriesBox.getChildren().addAll(hbDate, hbPrice);
            }

            // Buttons logic (PDF / Refund)
            File pdf = findPdfForSale(sale);
            if (detailsOpenPdfBtn != null && pdf != null) {
                detailsOpenPdfBtn.setDisable(false);
                detailsOpenPdfBtn.setVisible(true);
                detailsOpenPdfBtn.setManaged(true);
                detailsOpenPdfBtn.setText("View Invoice");
                detailsOpenPdfBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                detailsOpenPdfBtn.setOnAction(ev -> {
                    try {
                        java.awt.Desktop.getDesktop().open(pdf);
                    } catch (Exception ex) {
                    }
                });
            }
            if (detailsRefundBtn != null) {
                detailsRefundBtn.setVisible(true);
                detailsRefundBtn.setManaged(true);
                // ensure refund button action consistent
                detailsRefundBtn.setOnAction(ev -> {
                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Confirm refund?", ButtonType.YES, ButtonType.NO);
                    conf.setTitle("Confirm Refund");
                    conf.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            try {
                                saleService.returnCar(sale);
                                InvoiceGenerator.generateSaleRefundReceipt(sale.getSaleId(), sale.getCar(), sale.getCustomer(), sale.getPrice());
                                showAlert("Success", "Car refunded successfully");
                                buildCaches();
                                loadCarsWithSearch();
                                tableCars.refresh();
                                updateDetailsPaneForCar(car);
                            } catch (Exception ex) {
                                showAlert("Error", ex.getMessage());
                            }
                        }
                    });
                });
            }

            showDetailsPaneAnimated();
            return;
        }

        // 2. RENTED / RESERVED LOGIC
        List<Rental> rentals = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar() != null && r.getCar().getId().equals(car.getId()))
                .filter(r -> !r.getEndDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Rental::getStartDate))
                .collect(Collectors.toList());

        if (!rentals.isEmpty()) {
            detailsStatusLbl.setText("Status: Rented / Reserved");
            detailsStatusLbl.setStyle("-fx-text-fill: " + COLOR_SILVER_STATUS + "; -fx-font-size: 13px;");

            boolean first = true;
            for (Rental r : rentals) {
                VBox entry = new VBox(4);
                entry.setStyle("-fx-padding:6; -fx-background-radius:6; -fx-border-radius:6; -fx-background-color: transparent;");

                // Date Range (White) + Copy Icon
                String rangeStr = (r.getStartDate() != null ? r.getStartDate().format(dateFmt) : "?")
                        + " → " + (r.getEndDate() != null ? r.getEndDate().format(dateFmt) : "?");

                Label lblRangeText = new Label("Rental: " + rangeStr);
                lblRangeText.setStyle("-fx-text-fill: " + COLOR_WHITE_VALUE + "; -fx-font-size: 13px; -fx-font-weight: bold;");

                // Copy Button next to the range (White Color)
                Button btnCopyRange = createCopyButtonForValue(rangeStr, COLOR_WHITE_VALUE, "Copy Date Range");

                HBox hbRange = new HBox(6, lblRangeText, btnCopyRange);
                hbRange.setAlignment(Pos.CENTER_LEFT);

                String byName = (r.getCustomer() != null ? r.getCustomer().getName() : "N/A");
                String phone = (r.getCustomer() != null ? r.getCustomer().getPhone() : "N/A");

                // Name & Phone ALWAYS Gold
                HBox hbBy = createLabeledValueWithCopy("By:", byName, COLOR_WHEAT_LABEL, COLOR_GOLD, "Copy Name");
                HBox hbPhone = createLabeledValueWithCopy("Phone:", phone, COLOR_WHEAT_LABEL, COLOR_GOLD, "Copy Phone");

                // Price (Green)
                double total = r.getTotalPrice();
                String totalFmt = NumberFormat.getCurrencyInstance(Locale.US).format(total);
                Label lblPrice = new Label("Price: " + totalFmt);
                lblPrice.setStyle("-fx-text-fill: " + COLOR_GREEN_PRICE + "; -fx-font-size: 12px; -fx-font-weight:bold;");
                HBox hbPrice = new HBox(8, lblPrice, createCopyButtonForValue(totalFmt, COLOR_GREEN_PRICE, "Copy Price"));
                hbPrice.setAlignment(Pos.CENTER_LEFT);

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_LEFT);

                // Open contract specific to this rental
                Button openBtn = new Button("View Contract");
                openBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;"); // enforced color
                File pdfForThisRental = findPdfForRental(r);
                openBtn.setDisable(pdfForThisRental == null);
                openBtn.setOnAction(ev -> {
                    File f = findPdfForRental(r);
                    if (f != null) {
                        try {
                            java.awt.Desktop.getDesktop().open(f);
                        } catch (Exception ex) {
                        }
                    } else {
                        showAlert("Info", "No contract PDF found for this rental.");
                    }
                });

                // decide which action button to show (Return for active, Cancel for future)
                if (!LocalDate.now().isBefore(r.getStartDate()) && !LocalDate.now().isAfter(r.getEndDate())) {
                    // active now -> Return
                    Button returnBtn = new Button("Return");
                    returnBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                    returnBtn.setOnAction(ev -> {
                        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Confirm return?", ButtonType.YES, ButtonType.NO);
                        conf.setTitle("Confirm Return");
                        conf.showAndWait().ifPresent(resp -> {
                            if (resp == ButtonType.YES) {
                                try {
                                    rentalService.returnCar(r);
                                    InvoiceGenerator.generateReturnReceipt(r.getRentalId(), r.getCar(), r.getCustomer());
                                    showAlert("Success", "Car returned successfully!");
                                    buildCaches();
                                    loadCarsWithSearch();
                                    tableCars.refresh();
                                    updateDetailsPaneForCar(car);
                                } catch (Exception ex) {
                                    showAlert("Error", ex.getMessage());
                                }
                            }
                        });
                    });
                    actions.getChildren().addAll(openBtn, returnBtn);
                } else {
                    // future -> Cancel Reservation
                    Button cancelBtn = new Button("Cancel Reservation");
                    cancelBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
                    cancelBtn.setOnAction(ev -> {
                        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Confirm cancel reservation?", ButtonType.YES, ButtonType.NO);
                        conf.setTitle("Confirm Cancel");
                        conf.showAndWait().ifPresent(resp -> {
                            if (resp == ButtonType.YES) {
                                try {
                                    rentalService.returnCar(r); // reuse returnCar as cancel
                                    InvoiceGenerator.generateReturnReceipt(r.getRentalId(), r.getCar(), r.getCustomer());
                                    showAlert("Success", "Reservation cancelled successfully");
                                    buildCaches();
                                    loadCarsWithSearch();
                                    tableCars.refresh();
                                    updateDetailsPaneForCar(car);
                                } catch (Exception ex) {
                                    showAlert("Error", ex.getMessage());
                                }
                            }
                        });
                    });
                    actions.getChildren().addAll(openBtn, cancelBtn);
                }

                entry.getChildren().addAll(hbRange, hbBy, hbPhone, hbPrice, actions);

                if (detailsEntriesBox != null) {
                    if (!first) {
                        // golden separator
                        Separator sep = new Separator();
                        sep.setStyle("-fx-background-color: #fcd513; -fx-pref-height: 2;");
                        detailsEntriesBox.getChildren().add(sep);
                    }
                    detailsEntriesBox.getChildren().add(entry);
                }
                first = false;
            }

            showDetailsPaneAnimated();
            return;
        }

        // else available -> hide
        hideDetailsPaneAnimated();
        detailsCarIdDisplayed = null;
    }

    // helpers to find PDF files (improved matching to avoid wrong file)
    private File findPdfForRental(Rental r) {
        if (r == null) {
            return null;
        }
        String rentalId = r.getRentalId();
        File dir = new File("invoices/Rentals");
        if (!dir.exists() || dir.listFiles() == null) {
            return null;
        }

        // prefer exact-ish match: rental id surrounded by non-alphanumeric chars or filename equals id
        Pattern p = Pattern.compile("(?i)(?<![A-Za-z0-9])" + Pattern.quote(rentalId) + "(?![A-Za-z0-9])");
        for (File f : dir.listFiles()) {
            String name = f.getName();
            Matcher m = p.matcher(name);
            if (m.find()) {
                return f;
            }
        }
        // fallback: contains
        for (File f : dir.listFiles()) {
            if (f.getName().toLowerCase().contains(rentalId.toLowerCase())) {
                return f;
            }
        }
        return null;
    }

    private File findPdfForSale(Sale s) {
        if (s == null) {
            return null;
        }
        String saleId = s.getSaleId();
        File dir = new File("invoices/Sales");
        if (!dir.exists() || dir.listFiles() == null) {
            return null;
        }

        Pattern p = Pattern.compile("(?i)(?<![A-Za-z0-9])" + Pattern.quote(saleId) + "(?![A-Za-z0-9])");
        for (File f : dir.listFiles()) {
            String name = f.getName();
            Matcher m = p.matcher(name);
            if (m.find()) {
                return f;
            }
        }
        // fallback
        for (File f : dir.listFiles()) {
            if (f.getName().toLowerCase().contains(saleId.toLowerCase())) {
                return f;
            }
        }
        return null;
    }

    // FXML handlers for details pane buttons
    @FXML
    void onDetailsOpenPdf(ActionEvent event) {
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car == null) {
            return;
        }

        // If detailsOpenPdfBtn is visible and enabled it should already have setOnAction; fallback:
        List<Rental> rentals = rentalService.getAllRentals().stream()
                .filter(r -> r.getCar() != null && r.getCar().getId().equals(car.getId()))
                .filter(r -> !r.getEndDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Rental::getStartDate))
                .collect(Collectors.toList());
        if (!rentals.isEmpty() && rentals.size() == 1) {
            File f = findPdfForRental(rentals.get(0));
            if (f != null) try {
                java.awt.Desktop.getDesktop().open(f);
            } catch (Exception e) {
            }
            return;
        }
        Sale s = saleByCarId.get(car.getId());
        if (s != null) {
            File f = findPdfForSale(s);
            if (f != null) try {
                java.awt.Desktop.getDesktop().open(f);
            } catch (Exception e) {
            }
        }
    }

    @FXML
    void onDetailsRefund(ActionEvent event) {
        // This is handled dynamically in updateDetailsPaneForCar
        // but kept here if FXML triggers it directly
        Car car = tableCars.getSelectionModel().getSelectedItem();
        if (car == null) {
            return;
        }
        Sale s = saleByCarId.get(car.getId());
        if (s == null) {
            showAlert("Info", "No sale found for this car.");
            return;
        }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Confirm refund?", ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirm Refund");
        conf.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    saleService.returnCar(s);
                    // generate sale refund receipt PDF
                    InvoiceGenerator.generateSaleRefundReceipt(s.getSaleId(), s.getCar(), s.getCustomer(), s.getPrice());
                    showAlert("Success", "Car refunded successfully");
                    // refresh
                    buildCaches();
                    loadCarsWithSearch();
                    tableCars.refresh();
                    updateDetailsPaneForCar(car);
                } catch (Exception e) {
                    showAlert("Error", e.getMessage());
                }
            }
        });
    }
}