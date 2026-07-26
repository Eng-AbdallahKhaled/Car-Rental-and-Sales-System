package com.carapp.car_rental_and_sales_system.controller;

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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ArchivedCustomerController {

    @FXML private VBox rootPane;
    @FXML private TableView<Customer> tableArchived;
    @FXML private TableColumn<Customer, String> colId, colName, colNationalId, colPhone; // ✅ إضافة عمود الرقم القومي
    @FXML private Button btnReactivate, btnReactivateAll, btnViewTransactions;
    @FXML private TextField txtSearch; // ✅ حقل البحث المضاف

    private final CustomerService customerService = new CustomerService();
    private final RentalService rentalService = new RentalService();
    private final SaleService saleService = new SaleService();
    
    // متغير الصلاحية الافتراضي
    private String currentUserRole = "Authorized";

    // ✅ دالة استقبال الصلاحية وتطبيق القيود
    public void setSession(String role) {
        this.currentUserRole = role;
        
        if ("Admin".equals(currentUserRole)) {
            // إخفاء زر "تفعيل الكل" عن الأدمن العادي
            if (btnReactivateAll != null) {
                btnReactivateAll.setVisible(false);
                btnReactivateAll.setManaged(false); // لإزالة مكانه من الواجهة
            }
        }
    }

    @FXML
    public void initialize() {
        tableArchived.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // الترقيم التلقائي للأرشيف (ARC-1, ARC-2...)
        colId.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (this.getTableRow() != null && !empty) {
                    setText("ARC-" + (this.getTableRow().getIndex() + 1));
                } else setText(null);
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if(colNationalId != null) colNationalId.setCellValueFactory(new PropertyValueFactory<>("nationalId")); // ✅ ربط العمود
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        
        loadArchivedData();
        
        // مراقبة اختيار الجدول لتفعيل الأزرار
        tableArchived.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = (newVal != null);
            
            if (btnReactivate != null) btnReactivate.setDisable(!hasSelection);
            
            // تفعيل زر المعاملات فقط إذا كان للعميل تاريخ سابق
            if (btnViewTransactions != null) {
                btnViewTransactions.setDisable(!hasSelection || !hasHistory(newVal));
            }
        });
    }

    // ✅ دالة تصدير الجدول إلى إكسيل
    @FXML
    void btnExportToExcelAction(ActionEvent event) {
        ExportUtil.exportTableToExcel(tableArchived, rootPane.getScene().getWindow(), "Archived_Customers");
    }

    private boolean hasHistory(Customer c) {
        if (c == null) return false;
        // ✅ الاعتماد على الرقم القومي في البحث
        return rentalService.getAllRentals().stream().anyMatch(r -> r.getCustomer().getNationalId() != null && r.getCustomer().getNationalId().equals(c.getNationalId())) ||
               saleService.getAllSales().stream().anyMatch(s -> s.getCustomer().getNationalId() != null && s.getCustomer().getNationalId().equals(c.getNationalId()));
    }

    private void loadArchivedData() {
        ObservableList<Customer> list = FXCollections.observableArrayList(customerService.getArchivedCustomers());
        
        // ✅ منطق الفلترة (البحث)
        FilteredList<Customer> filteredData = new FilteredList<>(list, p -> true);
        
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(customer -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    
                    String lowerCaseFilter = newValue.toLowerCase();
                    
                    // البحث بالاسم (بداية الكلمات)
                    String[] nameParts = customer.getName().toLowerCase().split(" ");
                    for (String part : nameParts) {
                        if (part.startsWith(lowerCaseFilter)) return true;
                    }
                    
                    // البحث برقم الهاتف أو الرقم القومي
                    if (customer.getPhone().contains(lowerCaseFilter) || (customer.getNationalId() != null && customer.getNationalId().contains(lowerCaseFilter))) return true;
                    
                    return false;
                });
            });
        }
        
        SortedList<Customer> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableArchived.comparatorProperty());
        tableArchived.setItems(sortedData);
        
        // زر تفعيل الكل يكون معطلاً إذا كانت القائمة فارغة (للمصرح لهم فقط)
        if (btnReactivateAll != null && !"Admin".equals(currentUserRole)) {
            btnReactivateAll.setDisable(list.isEmpty());
        }
    }

    @FXML void btnViewTransactionsAction(ActionEvent event) {
        Customer selected = tableArchived.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showTransactionsDialog(selected); 
        }
    }

    // ✅ دالة عرض المعاملات
    private void showTransactionsDialog(Customer customer) {
        Stage stage = new Stage();
        stage.setTitle("History: " + customer.getName());
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15); 
        layout.setPadding(new Insets(20)); 
        layout.setStyle("-fx-background-color: #1a1a1a;");
        
        Label lblTitle = new Label("History for " + customer.getName());
        lblTitle.setStyle("-fx-text-fill: #fcd513; -fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<CustomerController.TransactionRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CustomerController.TransactionRow, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        TableColumn<CustomerController.TransactionRow, LocalDate> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());
        TableColumn<CustomerController.TransactionRow, String> colCar = new TableColumn<>("Car");
        colCar.setCellValueFactory(cell -> cell.getValue().carInfoProperty());
        TableColumn<CustomerController.TransactionRow, Double> colPrice = new TableColumn<>("Amount");
        colPrice.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());
        colPrice.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double p, boolean e) { super.updateItem(p, e); if(e||p==null) setText(null); else setText(NumberFormat.getCurrencyInstance(Locale.US).format(p)); } });

        table.getColumns().addAll(colType, colDate, colCar, colPrice);

        // ✅ Runnable للتحديث
        Runnable refreshTable = () -> {
            List<CustomerController.TransactionRow> rows = new ArrayList<>();
            // ✅ فلترة المعاملات بالرقم القومي
            rentalService.getAllRentals().stream().filter(r -> r.getCustomer().getNationalId() != null && r.getCustomer().getNationalId().equals(customer.getNationalId()))
                .forEach(r -> rows.add(new CustomerController.TransactionRow("Rental", r.getContractDate(), r.getRentalId(), r.getCar().getBrand() + " " + r.getCar().getModel(), r.getTotalPrice(), "Rentals", r)));
            
            saleService.getAllSales().stream().filter(s -> s.getCustomer().getNationalId() != null && s.getCustomer().getNationalId().equals(customer.getNationalId()))
                .forEach(s -> rows.add(new CustomerController.TransactionRow("Sale", s.getSaleDate(), s.getSaleId(), s.getCar().getBrand() + " " + s.getCar().getModel(), s.getPrice(), "Sales", s)));
            
            rows.sort(Comparator.comparing(CustomerController.TransactionRow::getDate).reversed());
            table.setItems(FXCollections.observableArrayList(rows));
        };
        refreshTable.run();

        Button btnOpenPdf = new Button("View Contract");
        btnOpenPdf.setDisable(true); btnOpenPdf.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button btnCancel = new Button("Action");
        btnCancel.setDisable(true); btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                btnOpenPdf.setDisable(findPdfFile(newVal) == null);
                btnCancel.setDisable(false);
                btnCancel.setText(newVal.getType().equals("Sale") ? "Refund" : "Return");
            } else { btnOpenPdf.setDisable(true); btnCancel.setDisable(true); }
        });

        btnOpenPdf.setOnAction(e -> {
            File pdf = findPdfFile(table.getSelectionModel().getSelectedItem());
            if (pdf != null) { try { Desktop.getDesktop().open(pdf); } catch (Exception ex) {} }
        });

        // ✅ زر الإلغاء مع التحديث الفوري
        btnCancel.setOnAction(e -> {
            CustomerController.TransactionRow sel = table.getSelectionModel().getSelectedItem();
            if (sel.getObj() instanceof Rental) {
                rentalService.returnCar((Rental) sel.getObj());
                InvoiceGenerator.generateReturnReceipt(sel.getId(), ((Rental)sel.getObj()).getCar(), customer);
            } else {
                saleService.returnCar((Sale) sel.getObj());
                InvoiceGenerator.generateSaleRefundReceipt(sel.getId(), ((Sale)sel.getObj()).getCar(), customer, sel.getPrice());
            }
            refreshTable.run(); // تحديث الجدول
            loadArchivedData(); // تحديث الخلفية
            new Alert(Alert.AlertType.INFORMATION, "Transaction cancelled successfully.").showAndWait();
        });

        HBox footer = new HBox(15, btnCancel, btnOpenPdf); footer.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().addAll(lblTitle, table, footer);
        
        Scene scene = new Scene(layout, 850, 500);
        scene.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ESCAPE) stage.close(); });
        
        stage.setScene(scene); 
        stage.show();
    }

    private File findPdfFile(CustomerController.TransactionRow row) {
        File dir = new File("invoices/" + row.getFolder());
        if (!dir.exists() || dir.listFiles() == null) return null;
        for (File f : dir.listFiles()) if (f.getName().contains(row.getId())) return f;
        return null;
    }

    @FXML void btnReactivateAction(ActionEvent event) {
        Customer c = tableArchived.getSelectionModel().getSelectedItem();
        if (c != null) { 
            customerService.reactivateCustomer(c); 
            loadArchivedData(); 
            showAlert("Success", "Customer Reactivated Successfully!");
        }
    }

    @FXML void btnReactivateAllAction(ActionEvent event) {
        if (tableArchived.getItems().isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Reactivate All");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will move ALL archived customers back to the active list.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            customerService.getArchivedCustomers().forEach(customerService::reactivateCustomer);
            loadArchivedData();
            showAlert("Success", "All customers have been reactivated.");
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}