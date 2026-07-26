package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.App;
import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Rental;
import com.carapp.car_rental_and_sales_system.model.Sale;
import com.carapp.car_rental_and_sales_system.service.CarService;
import com.carapp.car_rental_and_sales_system.service.RentalService;
import com.carapp.car_rental_and_sales_system.service.SaleService;
import com.carapp.car_rental_and_sales_system.service.UserService;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * الكنترولر الرئيسي: يدير القائمة الجانبية ومنطقة العرض الرئيسية والداشبورد
 */
public class MainController implements Initializable {

    // =========================================================================
    // تعريف عناصر الواجهة الأصلية (الأزرار والبانيل)
    // =========================================================================
    @FXML private BorderPane mainBorderPane;
    
    @FXML private Button btnHome; // زرار الداشبورد الجديد اللي ضفناه
    @FXML private Button btnCars;
    @FXML private Button btnCustomers;
    @FXML private Button btnArchives; 
    @FXML private Button btnRentals;
    @FXML private Button btnSales;
    @FXML private Button btnChangePassword;

    // =========================================================================
    // تعريف عناصر الداشبورد والإحصائيات
    // =========================================================================
    @FXML private Label lblTotalCars;
    @FXML private Label lblRentedCars;
    @FXML private Label lblSoldCars;
    @FXML private Label lblTotalRevenue;
    @FXML private PieChart pieChartCars;
    @FXML private BarChart<String, Number> barChartRevenue;
    @FXML private VBox vboxAlerts;

    // =========================================================================
    // المتغيرات والخدمات
    // =========================================================================
    private String currentViewName = "home"; // خلينا الديفولت هو الـ home (الداشبورد)
    private Node dashboardView; // هنا هنخزن واجهة الداشبورد عشان لما نتنقل نعرف نرجعلها بسرعة

    private final UserService userService;
    private final CarService carService;
    private final RentalService rentalService;
    private final SaleService saleService;
    
    private String currentUserRole;
    private String currentUsername;

    public MainController() {
        this.userService = new UserService();
        this.carService = new CarService();
        this.rentalService = new RentalService();
        this.saleService = new SaleService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (mainBorderPane != null) {
            mainBorderPane.setOnMousePressed(event -> mainBorderPane.requestFocus());
            
            // أول ما الشاشة تفتح، بناخد الـ Center الحالي (اللي هو الداشبورد) ونحفظه في المتغير ده
            dashboardView = mainBorderPane.getCenter();
        }
        
        // بنفرش بيانات الشارتس والأرقام
        refreshDashboard();
    }
    
    /**
     * إعداد الجلسة عند تسجيل الدخول
     */
    public void initSession(String role, String username) {
        this.currentUserRole = role;
        this.currentUsername = username;
        
        // مش محتاجين نفتح صفحة العربيات تلقائي خلاص، لأن الداشبورد هو اللي هيظهر أول حاجة
    }

    // =========================================================================
    // أزرار التنقل الجانبية
    // =========================================================================

    @FXML
    void btnShowHome(ActionEvent event) {
        // لو إحنا أصلا في الداشبورد متعملش حاجة
        if (currentViewName.equals("home")) return;
        
        // بنرجع الديزاين اللي حفظناه في الأول للسنتر تاني
        mainBorderPane.setCenter(dashboardView);
        currentViewName = "home";
        updateActiveButtonStyle((Button) event.getSource());
        
        // بنعمل ريفريش عشان لو كان في عربية اتباعت واحنا بنقلب، تسمع هنا في الداشبورد
        refreshDashboard(); 
    }

    @FXML
    void btnShowCars(ActionEvent event) {
        handleNavigation((Button) event.getSource(), "cars_view");
    }

    @FXML
    void btnShowCustomers(ActionEvent event) {
        handleNavigation((Button) event.getSource(), "customers_view");
    }

    @FXML
    void btnShowArchives(ActionEvent event) {
        handleNavigation((Button) event.getSource(), "archived_customers_view");
    }

    @FXML
    void btnShowRentals(ActionEvent event) {
        handleNavigation((Button) event.getSource(), "rentals_view");
    }

    @FXML
    void btnShowSales(ActionEvent event) {
        handleNavigation((Button) event.getSource(), "sales_view");
    }
    
    @FXML
    void btnChangePasswordAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("change_password.fxml"));
            Parent root = loader.load();
            
            // الحصول على الكنترولر وتمرير البيانات
            ChangePasswordController controller = loader.getController();
            String currentHint = userService.getHintForUser(currentUsername);
            controller.initData(userService, currentUsername, currentHint);
            
            controller.setOnUpdateSuccess(() -> {
                // (يمكن إضافة منطق لتحديث الواجهة هنا إذا لزم الأمر)
            });

            Stage stage = new Stage();
            stage.setTitle("Change Password");
            stage.initModality(Modality.APPLICATION_MODAL); // يمنع التفاعل مع الخلفية
            
            // إضافة أيقونة البرنامج
            try {
                stage.getIcons().add(new Image(App.class.getResourceAsStream("logo.jpg")));
            } catch (Exception e) {}

            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open change password window.");
        }
    }

    /**
     * منطق التنقل: تحميل الواجهة وتحديث شكل الزر
     */
    private void handleNavigation(Button clickedButton, String fxmlName) {
        if (currentViewName.equals(fxmlName)) {
            return; // لا نعد التحميل إذا كنا في نفس الصفحة
        }

        loadView(fxmlName);
        currentViewName = fxmlName;
        updateActiveButtonStyle(clickedButton);
    }

    /**
     * تحميل ملف FXML وتمرير الصلاحيات للكنترولر
     */
    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
            Parent view = loader.load();
            
            // تمرير صلاحيات المستخدم (Session) للكنترولر المحمّل
            Object controller = loader.getController();
            
            if (controller instanceof CarController) {
                ((CarController) controller).setSession(currentUserRole);
            } else if (controller instanceof CustomerController) {
                ((CustomerController) controller).setSession(currentUserRole);
            } else if (controller instanceof ArchivedCustomerController) {
                ((ArchivedCustomerController) controller).setSession(currentUserRole);
            }

            mainBorderPane.setCenter(view);
 
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Could not load view: " + fxmlName + "\n" + e.getMessage());
        }
    }

    // تمييز الزر النشط وإعادة تعيين الباقي
    private void updateActiveButtonStyle(Button activeButton) {
        Button[] menuButtons = {btnHome, btnCars, btnCustomers, btnArchives, btnRentals, btnSales};
        
        for (Button btn : menuButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active-menu-btn");
                // إعادة الستايل الافتراضي
                btn.setStyle("-fx-background-color: #1C1F26; -fx-background-radius: 14; -fx-text-fill: #D4AF37; -fx-border-color: #8C7A2E; -fx-border-radius: 14;");
            }
        }

        if (activeButton != null) {
            activeButton.getStyleClass().add("active-menu-btn");
            // إضافة التمييز (خط ذهبي على الشمال بيدي شياكة)
            activeButton.setStyle("-fx-background-color: #1C1F26; -fx-background-radius: 14; -fx-text-fill: #D4AF37; -fx-border-color: #fcd513; -fx-border-width: 0 0 0 5; -fx-border-radius: 14;");
        }
    }

    @FXML
    void btnLogoutAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("login_view.fxml"));
            Parent view = loader.load();
            
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            
            // الحفاظ على حجم ومكان النافذة لتجنب الرعشة
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            Scene scene = new Scene(view, width, height);
            try {
                scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
            } catch (Exception e) {}
            
            stage.setScene(scene);
            if (wasMaximized) stage.setMaximized(true);
            
            // لا نستخدم show() لعدم الوميض
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // دوال الداشبورد (الإحصائيات والتنبيهات)
    // =========================================================================

    public void refreshDashboard() {
        // بنسحب كل الداتا عشان نحدث الأرقام
        List<Car> allCars = carService.getAllCars();
        List<Rental> allRentals = rentalService.getAllRentals();
        List<Sale> allSales = saleService.getAllSales();

        int totalCars = allCars.size();
        int soldCarsCount = allSales.size();
        
        int rentedCarsCount = (int) allRentals.stream()
                .filter(r -> r.getEndDate() != null && !r.getEndDate().isBefore(LocalDate.now()))
                .count();

        int availableCarsCount = totalCars - soldCarsCount - rentedCarsCount;

        double totalRevenue = 0.0;
        for (Sale sale : allSales) totalRevenue += sale.getPrice();
        for (Rental rental : allRentals) totalRevenue += rental.getTotalPrice();

        lblTotalCars.setText(String.valueOf(totalCars));
        lblSoldCars.setText(String.valueOf(soldCarsCount));
        lblRentedCars.setText(String.valueOf(rentedCarsCount));
        lblTotalRevenue.setText(NumberFormat.getCurrencyInstance(Locale.US).format(totalRevenue));

        // تظبيط الـ Pie Chart 
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Available (" + availableCarsCount + ")", availableCarsCount),
                new PieChart.Data("Rented (" + rentedCarsCount + ")", rentedCarsCount),
                new PieChart.Data("Sold (" + soldCarsCount + ")", soldCarsCount)
        );
        pieChartCars.setData(pieChartData);

        // تظبيط الـ Bar Chart للإيرادات
        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName("Sales Revenue");
        
        XYChart.Series<String, Number> rentalSeries = new XYChart.Series<>();
        rentalSeries.setName("Rental Revenue");

        double totalSalesMoney = allSales.stream().mapToDouble(Sale::getPrice).sum();
        double totalRentalMoney = allRentals.stream().mapToDouble(Rental::getTotalPrice).sum();

        salesSeries.getData().add(new XYChart.Data<>("Total", totalSalesMoney));
        rentalSeries.getData().add(new XYChart.Data<>("Total", totalRentalMoney));

        barChartRevenue.getData().clear();
        barChartRevenue.getData().addAll(salesSeries, rentalSeries);

        // تظبيط نظام التنبيهات الذكي
        vboxAlerts.getChildren().clear();
        boolean hasAlerts = false;

        LocalDate today = LocalDate.now();
        for (Rental r : allRentals) {
            // لو تاريخ نهاية العقد خلص والعميل لسه مأجرها، بنرمي تنبيه فورا
            if (r.getEndDate() != null && r.getEndDate().isBefore(today)) {
                addAlertMessage("⚠️ Overdue Rental Alert!", 
                        "Customer " + r.getCustomer().getName() + " is late returning the " + 
                        r.getCar().getBrand() + " " + r.getCar().getModel() + 
                        ". Expected return date was: " + r.getEndDate(), "#e74c3c");
                hasAlerts = true;
            }
        }

        if (!hasAlerts) {
            addAlertMessage("✅ System Clear", "No pending alerts or overdue cars. Everything is running smoothly!", "#2ecc71");
        }
    }

    // دالة بترسم ستايل التنبيهات وتحطها في القائمة
    private void addAlertMessage(String title, String details, String colorHex) {
        VBox alertBox = new VBox(5);
        alertBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: " + colorHex + "; -fx-border-width: 0 0 0 5; -fx-padding: 10; -fx-background-radius: 0 5 5 0;");
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label lblDetails = new Label(details);
        lblDetails.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        lblDetails.setWrapText(true);

        alertBox.getChildren().addAll(lblTitle, lblDetails);
        vboxAlerts.getChildren().add(alertBox);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}