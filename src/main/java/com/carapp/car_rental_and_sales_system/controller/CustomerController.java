package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.App;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.util.InvoiceGenerator;
import com.carapp.car_rental_and_sales_system.util.ExportUtil;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CustomerController {

    @FXML private VBox rootPane;
    @FXML private TextField txtSearch, txtName, txtNationalId, txtPhone;
    
    // ✅ الحقول الجديدة للصورة والنوع
    @FXML private ComboBox<String> comboGender;
    @FXML private ImageView imgCustomerPreview;
    
    @FXML private Button btnUpdateCustomer;
    @FXML private Button btnArchiveCustomer;
    @FXML private Button btnDeleteCustomer;
    @FXML private Button btnViewTransactions; 
    @FXML private Button btnDeactivateAll; 

    @FXML private TableView<Customer> tableCustomers;
    @FXML private TableColumn<Customer, String> colId, colName, colNationalId, colPhone, colGender;

    private final CustomerService customerService;
    private final RentalService rentalService;
    private final SaleService saleService;
    private String currentUserRole = "Authorized";
    
    // متغير لتخزين مسار الصورة مؤقتاً قبل الحفظ
    private String currentSelectedImagePath = null;

    public CustomerController() {
        this.customerService = new CustomerService();
        this.rentalService = new RentalService();
        this.saleService = new SaleService();
    }

    public void setSession(String role) {
        this.currentUserRole = role;
        if ("Admin".equals(currentUserRole)) {
            btnUpdateCustomer.setVisible(false); btnUpdateCustomer.setManaged(false);
            btnDeleteCustomer.setVisible(false); btnDeleteCustomer.setManaged(false);
            btnDeactivateAll.setVisible(false); btnDeactivateAll.setManaged(false);
        }
    }

    @FXML
    public void initialize() {
        tableCustomers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ✅ تهيئة خيارات النوع
        comboGender.getItems().addAll("Male", "Female", "Not Specified");
        comboGender.setValue("Not Specified");

        colId.setCellValueFactory(cellData -> {
            Customer currentCustomer = cellData.getValue();
            List<Customer> allActive = customerService.getActiveCustomers();
            int index = allActive.indexOf(currentCustomer);
            if (index >= 0) {
                return new SimpleStringProperty("CUST-" + (index + 1));
            }
            return new SimpleStringProperty("Unknown");
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if(colNationalId != null) colNationalId.setCellValueFactory(new PropertyValueFactory<>("nationalId"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if(colGender != null) colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        
        loadData();
        
        tableCustomers.getSortOrder().add(colName);

        tableCustomers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                txtName.setText(newV.getName()); 
                if(txtNationalId != null) txtNationalId.setText(newV.getNationalId());
                txtPhone.setText(newV.getPhone());
                
                // ✅ تعبئة النوع
                if(newV.getGender() != null) {
                    comboGender.setValue(newV.getGender());
                } else {
                    comboGender.setValue("Not Specified");
                }
                
                // ✅ تحميل وعرض صورة العميل
                currentSelectedImagePath = newV.getImagePath();
                loadCustomerImage(currentSelectedImagePath);
                
                boolean hasHistory = hasTransactions(newV);

                if (!"Admin".equals(currentUserRole)) {
                    btnUpdateCustomer.setDisable(false); 
                    btnArchiveCustomer.setDisable(false); 
                    
                    if (hasHistory) {
                        btnDeleteCustomer.setDisable(true); 
                        btnDeleteCustomer.setTooltip(new Tooltip("Cannot delete customer with active history. Use Archive instead."));
                    } else {
                        btnDeleteCustomer.setDisable(false); 
                        btnDeleteCustomer.setTooltip(null);
                    }
                } else {
                    btnArchiveCustomer.setDisable(false);
                }
                
                btnViewTransactions.setDisable(!hasHistory);
            } else {
                clearFields();
                btnViewTransactions.setDisable(true);
            }
        });
        
        if (btnDeactivateAll != null && !"Admin".equals(currentUserRole)) {
            btnDeactivateAll.setDisable(tableCustomers.getItems().isEmpty());
        }

        if (rootPane != null) rootPane.setOnMousePressed(event -> rootPane.requestFocus());
    }

    // ✅ دالة تصدير الجدول إلى إكسيل
    @FXML
    void btnExportToExcelAction(ActionEvent event) {
        ExportUtil.exportTableToExcel(tableCustomers, rootPane.getScene().getWindow(), "Active_Customers");
    }

    // ✅ دالة آمنة لتحميل الصورة أو الصورة الافتراضية
    private void loadCustomerImage(String path) {
        if (path != null && !path.isEmpty()) {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                imgCustomerPreview.setImage(new Image(imgFile.toURI().toString(), true));
                return;
            }
        }
        // تحميل الصورة الافتراضية
        try {
            Image defaultImg = new Image(App.class.getResourceAsStream("default_user.jpeg"));
            imgCustomerPreview.setImage(defaultImg);
        } catch (Exception e) {
            imgCustomerPreview.setImage(null);
        }
    }

    // ✅ دالة رفع الصورة ونسخها لمجلد customer_images
    @FXML void btnUploadImageAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Customer Photo");
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
                
                currentSelectedImagePath = "customer_images/" + newFileName;
                loadCustomerImage(currentSelectedImagePath);
                
            } catch (IOException e) {
                showAlert("Error", "Failed to upload image: " + e.getMessage());
            }
        }
    }

    private boolean hasTransactions(Customer c) {
        if (c == null) return false;
        return rentalService.getAllRentals().stream().anyMatch(r -> r.getCustomer().getNationalId() != null && r.getCustomer().getNationalId().equals(c.getNationalId())) ||
               saleService.getAllSales().stream().anyMatch(s -> s.getCustomer().getNationalId() != null && s.getCustomer().getNationalId().equals(c.getNationalId()));
    }

    private void loadData() {
        ObservableList<Customer> list = FXCollections.observableArrayList(customerService.getActiveCustomers());
        FilteredList<Customer> filtered = new FilteredList<>(list, p -> true);
        
        txtSearch.textProperty().addListener((obs, old, newVal) -> {
            filtered.setPredicate(c -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                String[] nameParts = c.getName().toLowerCase().split(" ");
                for (String part : nameParts) {
                    if (part.startsWith(f)) return true;
                }
                return c.getPhone().contains(f) || (c.getNationalId() != null && c.getNationalId().contains(f));
            });
        });
        
        SortedList<Customer> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableCustomers.comparatorProperty());
        tableCustomers.setItems(sorted);
        
        if (btnDeactivateAll != null && !"Admin".equals(currentUserRole)) {
            btnDeactivateAll.setDisable(list.isEmpty());
        }
    }

    @FXML void btnAddCustomer(ActionEvent event) {
        if (txtName.getText().trim().isEmpty() || txtNationalId == null || txtNationalId.getText().trim().isEmpty() || txtPhone.getText().trim().isEmpty()) {
            showAlert("Error", "Please enter Name, National ID, and Phone.");
            return;
        }
        if (!txtNationalId.getText().trim().matches("^\\d{14}$")) {
            showAlert("Error", "National ID must be exactly 14 digits.");
            return;
        }
        
        String gender = comboGender.getValue();
        
        // ✅ إضافة باستخدام كل الحقول الجديدة
        customerService.addCustomer(txtName.getText().trim(), txtNationalId.getText().trim(), txtPhone.getText().trim(), gender, currentSelectedImagePath);
        loadData(); 
        clearFields();
        showAlert("Success", "Customer Added Successfully!");
    }

    @FXML void btnUpdateCustomer(ActionEvent event) {
        Customer c = tableCustomers.getSelectionModel().getSelectedItem();
        if (c == null) return;
        
        if (txtName.getText().trim().isEmpty() || txtNationalId == null || txtNationalId.getText().trim().isEmpty() || txtPhone.getText().trim().isEmpty()) {
            showAlert("Error", "Please enter Name, National ID, and Phone.");
            return;
        }
        if (!txtNationalId.getText().trim().matches("^\\d{14}$")) {
            showAlert("Error", "National ID must be exactly 14 digits.");
            return;
        }

        c.setName(txtName.getText().trim()); 
        c.setNationalId(txtNationalId.getText().trim()); 
        c.setPhone(txtPhone.getText().trim()); 
        c.setGender(comboGender.getValue());
        c.setImagePath(currentSelectedImagePath);
        
        customerService.updateCustomer(c); 
        tableCustomers.refresh(); 
        clearFields();
        showAlert("Success", "Customer Updated!");
    }

    @FXML void btnDeactivateAllAction(ActionEvent event) {
        if (tableCustomers.getItems().isEmpty()) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Archive All");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will move ALL active customers to the archive.");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            customerService.getActiveCustomers().forEach(customerService::archiveCustomer);
            loadData();
            clearFields();
            showAlert("Success", "All customers moved to Archive.");
        }
    }

    @FXML void btnViewTransactionsAction(ActionEvent event) {
        Customer selected = tableCustomers.getSelectionModel().getSelectedItem();
        if (selected != null) showTransactionsDialog(selected);
    }

    public void showTransactionsDialog(Customer customer) {
        Stage stage = new Stage();
        stage.setTitle("Customer History: " + customer.getName());
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15); layout.setPadding(new Insets(20)); layout.setStyle("-fx-background-color: #1a1a1a;");
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
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(NumberFormat.getCurrencyInstance(Locale.US).format(price));
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
        btnOpenPdf.setDisable(true); btnOpenPdf.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnCancel = new Button("Action");
        btnCancel.setDisable(true); btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                btnOpenPdf.setDisable(findPdf(newVal) == null);
                btnCancel.setDisable(false);
                btnCancel.setText(newVal.getType().equals("Sale") ? "Refund" : "Return");
            } else {
                btnOpenPdf.setDisable(true); btnCancel.setDisable(true);
            }
        });

        btnOpenPdf.setOnAction(e -> {
            File f = findPdf(table.getSelectionModel().getSelectedItem());
            if (f != null) { try { Desktop.getDesktop().open(f); } catch (Exception ex) {} }
        });

        btnCancel.setOnAction(e -> {
            TransactionRow sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            
            if (sel.getObj() instanceof Rental) {
                rentalService.returnCar((Rental) sel.getObj());
                InvoiceGenerator.generateReturnReceipt(sel.getId(), ((Rental)sel.getObj()).getCar(), customer);
            } else {
                saleService.returnCar((Sale) sel.getObj());
                InvoiceGenerator.generateSaleRefundReceipt(sel.getId(), ((Sale)sel.getObj()).getCar(), customer, sel.getPrice());
            }
            
            showAlert("Success", "Transaction cancelled successfully.");
            refreshTable.run(); 
            loadData(); 
        });

        HBox footer = new HBox(15, btnCancel, btnOpenPdf); footer.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().addAll(lblTitle, table, footer);
        
        Scene scene = new Scene(layout, 850, 500);
        stage.setScene(scene); 
        stage.show();
    }

    private File findPdf(TransactionRow row) {
        File dir = new File("invoices/" + row.getFolder());
        if (!dir.exists() || dir.listFiles() == null) return null;
        for (File f : dir.listFiles()) if (f.getName().contains(row.getId())) return f;
        return null;
    }

    private void clearFields() {
        txtName.clear(); 
        if(txtNationalId != null) txtNationalId.clear(); 
        txtPhone.clear(); 
        comboGender.setValue("Not Specified");
        currentSelectedImagePath = null;
        loadCustomerImage(null); // يرجع للصورة الافتراضية
        tableCustomers.getSelectionModel().clearSelection();
        
        if (!"Admin".equals(currentUserRole)) {
            btnUpdateCustomer.setDisable(true); 
            btnArchiveCustomer.setDisable(true); 
            btnDeleteCustomer.setDisable(true);
        }
        btnViewTransactions.setDisable(true);
    }
    
    @FXML void btnArchiveCustomer(ActionEvent event) {
        Customer c = tableCustomers.getSelectionModel().getSelectedItem();
        if (c != null) { 
            customerService.archiveCustomer(c); 
            loadData(); 
            clearFields(); 
            showAlert("Success", "Customer moved to Archive.");
        }
    }
    
    @FXML void btnDeleteCustomer(ActionEvent event) {
        Customer c = tableCustomers.getSelectionModel().getSelectedItem();
        if (c != null) { 
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Customer");
            alert.setContentText("Are you sure you want to delete this customer permanently?");
            if (alert.showAndWait().get() == ButtonType.OK) {
                customerService.deletePermanent(c); 
                loadData(); 
                clearFields(); 
                showAlert("Success", "Customer Deleted Permanently.");
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
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

        public String getType() { return type.get(); }
        public SimpleStringProperty typeProperty() { return type; }

        public LocalDate getDate() { return date.get(); }
        public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
        
        public String getId() { return id.get(); }

        public String getCarInfo() { return carInfo.get(); }
        public SimpleStringProperty carInfoProperty() { return carInfo; }

        public Double getPrice() { return price.get(); }
        public SimpleDoubleProperty priceProperty() { return price; }
        
        public String getFolder() { return folder.get(); }
        public Object getObj() { return obj; }
    }
}