package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.util.ExportUtil; // ✅ إضافة كلاس التصدير للإكسيل
import com.carapp.car_rental_and_sales_system.util.InvoiceGenerator;
import java.awt.Desktop;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class SaleController {

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
    private TextField txtFinalPrice;
    @FXML
    private Label lblDate;

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
    private Button btnExportExcel; // ✅ زر الإكسيل الجديد

    @FXML
    private ImageView imgCarPreview;

    @FXML
    private TableView<Sale> tableSales;
    @FXML
    private TableColumn<Sale, String> colId;
    @FXML
    private TableColumn<Sale, String> colCar;
    @FXML
    private TableColumn<Sale, String> colCustomer;
    @FXML
    private TableColumn<Sale, Double> colPrice;
    @FXML
    private TableColumn<Sale, LocalDate> colDate;

    private final SaleService saleService;
    private final CarService carService;
    private final CustomerService customerService;

    private FilteredList<Car> filteredCars;
    private FilteredList<Customer> filteredCustomers;

    public SaleController() {
        this.saleService = new SaleService();
        this.carService = new CarService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        tableSales.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lblDate.setText(LocalDate.now().toString());
        loadData();
        setupComboBoxes();
        setupSearchFilters();

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

        tableSales.setRowFactory(tv -> new TableRow<Sale>() {
            @Override
            protected void updateItem(Sale item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    int currentIndex = getIndex();
                    if (currentIndex > 0) {
                        Sale prev = getTableView().getItems().get(currentIndex - 1);
                        if (!item.getSaleDate().isEqual(prev.getSaleDate())) {
                            setStyle("-fx-border-color: #fcd513; -fx-border-width: 2 0 0 0;");
                        } else {
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                } else {
                    setStyle("");
                }
            }
        });

        comboCars.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtFinalPrice.setText(String.valueOf(newVal.getSalePrice()));
            } else {
                txtFinalPrice.clear();
            }
            updateViewButtonsState(newVal);
            updateImagePreview(newVal);
        });

        tableSales.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
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

        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> rootPane.requestFocus());
        }
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

    private File findPdf(Sale sale) {
        if (sale == null) {
            return null;
        }
        File dir = new File("invoices/Sales");
        if (!dir.exists() || dir.listFiles() == null) {
            return null;
        }
        for (File f : dir.listFiles()) {
            if (f.getName().toLowerCase().endsWith(".pdf") && f.getName().contains(sale.getSaleId())) {
                return f;
            }
        }
        return null;
    }

    // ✅ دالة التصدير للإكسيل الخاصة بجدول المبيعات
    @FXML
    void btnExportToExcelAction(ActionEvent event) {
        ExportUtil.exportTableToExcel(tableSales, txtSearchTable.getScene().getWindow(), "Sales_Data");
    }

    @FXML
    void btnOpenInvoiceAction(ActionEvent event) {
        Sale sale = tableSales.getSelectionModel().getSelectedItem();
        File pdfFile = findPdf(sale);
        if (pdfFile != null) {
            try {
                Desktop.getDesktop().open(pdfFile);
            } catch (Exception e) {
                showAlert("Error", "Could not open PDF file.");
            }
        }
    }

    @FXML
    void btnSellCar(ActionEvent event) {
        try {
            Car selectedCar = comboCars.getValue();
            Customer selectedCustomer = comboCustomers.getValue();

            if (selectedCar == null || selectedCustomer == null || txtFinalPrice.getText().isEmpty()) {
                showAlert("Error", "Please select Car, Customer and Enter Price.");
                return;
            }

            double price = Double.parseDouble(txtFinalPrice.getText());
            saleService.sellCar(selectedCar, selectedCustomer, price);

            showAlert("Success", "Car Sold Successfully!");

            List<Sale> sales = saleService.getAllSales();
            Sale newSale = sales.get(sales.size() - 1);
            InvoiceGenerator.generateSaleInvoice(newSale.getSaleId(), selectedCar, selectedCustomer, price);

            loadData();
            txtFinalPrice.clear();
            comboCars.getSelectionModel().clearSelection();
            comboCustomers.getSelectionModel().clearSelection();
            updateViewButtonsState(null);

            txtSearchCar.clear();
            txtSearchCustomer.clear();
            if (txtSearchTable != null) {
                txtSearchTable.clear();
            }

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid Price Format.");
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    void btnReturnCarAction(ActionEvent event) {
        Sale sale = tableSales.getSelectionModel().getSelectedItem();
        if (sale == null) {
            return;
        }

        saleService.returnCar(sale);
        showAlert("Success", "Sale Refunded Successfully!");

        InvoiceGenerator.generateSaleRefundReceipt(sale.getSaleId(), sale.getCar(), sale.getCustomer(), sale.getPrice());

        loadData();
        updateViewButtonsState(null);

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

    private void loadData() {
        List<Sale> allSales = new ArrayList<>(saleService.getAllSales());
        allSales.sort(Comparator.comparing(Sale::getSaleDate).reversed());

        ObservableList<Sale> salesList = FXCollections.observableArrayList(allSales);
        FilteredList<Sale> filteredSales = new FilteredList<>(salesList, b -> true);

        if (txtSearchTable != null) {
            txtSearchTable.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredSales.setPredicate(sale -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerFilter = newValue.toLowerCase();
                    String searchStr = (sale.getSaleId() + " "
                            + sale.getCar().getBrand() + " "
                            + sale.getCar().getModel() + " "
                            + sale.getCustomer().getName()).toLowerCase();
                    String[] parts = searchStr.split(" ");
                    for (String part : parts) {
                        if (part.startsWith(lowerFilter)) {
                            return true;
                        }
                    }
                    return false;
                });
            });
        }

        SortedList<Sale> sortedData = new SortedList<>(filteredSales);
        sortedData.comparatorProperty().bind(tableSales.comparatorProperty());
        tableSales.setItems(sortedData);

        // ✅ 1. ترتيب السيارات أبجدياً
        List<Car> sortedCars = new ArrayList<>(carService.getAvailableCars());
        sortedCars.sort(Comparator.comparing(Car::getBrand, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Car::getModel, String.CASE_INSENSITIVE_ORDER));
        ObservableList<Car> availableCars = FXCollections.observableArrayList(sortedCars);
        filteredCars = new FilteredList<>(availableCars, p -> true);
        comboCars.setItems(filteredCars);

        // ✅ 2. ترتيب العملاء أبجدياً
        List<Customer> sortedCustomers = new ArrayList<>(customerService.getActiveCustomers());
        sortedCustomers.sort(Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER));
        ObservableList<Customer> activeCustomers = FXCollections.observableArrayList(sortedCustomers);
        filteredCustomers = new FilteredList<>(activeCustomers, p -> true);
        comboCustomers.setItems(filteredCustomers);
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
        colId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colCar.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCar().getBrand() + " " + cell.getValue().getCar().getModel()));
        colCustomer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomer().getName()));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        formatCurrencyColumn(colPrice);
    }

    private void formatCurrencyColumn(TableColumn<Sale, Double> column) {
        column.setCellFactory(tc -> new TableCell<Sale, Double>() {
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

    @FXML
    void btnTableShowDataAction(ActionEvent event) {
        Sale sale = tableSales.getSelectionModel().getSelectedItem();
        if (sale != null) {
            showCarDataDialog(sale.getCar());
        }
    }

    @FXML
    void btnTableShowImagesAction(ActionEvent event) {
        Sale sale = tableSales.getSelectionModel().getSelectedItem();
        if (sale != null) {
            showCarImagesWindow(sale.getCar());
        }
    }

    @FXML
    void btnShowDataAction(ActionEvent event) {
        Car car = comboCars.getValue();
        if (car != null) {
            showCarDataDialog(car);
        }
    }

    @FXML
    void btnShowImagesAction(ActionEvent event) {
        Car car = comboCars.getValue();
        if (car != null) {
            showCarImagesWindow(car);
        }
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
        content.getChildren().add(createDetailLabel("Sale Price: ", currency.format(car.getSalePrice())));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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

    private Label createDetailLabel(String title, String value) {
        Label lbl = new Label(title + (value == null ? "N/A" : value));
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        return lbl;
    }

    private void setupComboBoxes() {
        Callback<ListView<Car>, ListCell<Car>> carCellFactory = param -> new ListCell<Car>() {
            @Override
            protected void updateItem(Car item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getBrand() + " " + item.getModel());
                }
            }
        };
        comboCars.setButtonCell(carCellFactory.call(null));
        comboCars.setCellFactory(carCellFactory);

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

    private void clearFields() {
        txtFinalPrice.clear();
        comboCars.getSelectionModel().clearSelection();
        comboCustomers.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}