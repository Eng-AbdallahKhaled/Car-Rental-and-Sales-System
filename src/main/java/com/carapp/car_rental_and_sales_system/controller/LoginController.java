package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.App;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.carapp.car_rental_and_sales_system.model.User;
import com.carapp.car_rental_and_sales_system.service.CustomerService;
import com.carapp.car_rental_and_sales_system.service.UserService;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML private VBox rootPane;
    @FXML private ComboBox<String> comboRole;
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtNationalId; 
    @FXML private TextField txtPhone; 
    @FXML private PasswordField txtPassword; 
    @FXML private TextField txtVisiblePassword; 

    @FXML private Button btnTogglePassword;     
    @FXML private Button btnLogin;
    @FXML private Hyperlink linkForgotPassword;
    @FXML private CheckBox chkRememberMe;

    @FXML private Label lblMessage;
    @FXML private Label lblUserHint;
    @FXML private Label lblPassHint;
    @FXML private Label lblChangePassHint;

    private final UserService userService;
    private final CustomerService customerService;

    public LoginController() {
        this.userService = new UserService();
        this.customerService = new CustomerService();
    }

    @FXML
    public void initialize() {
        comboRole.getItems().addAll("Authorized", "Admin", "Customer");
        comboRole.setValue("Authorized");

        txtVisiblePassword.textProperty().bindBidirectional(txtPassword.textProperty());

        User savedUser = userService.getRememberedUser("Authorized");
        if (savedUser != null) {
            txtUsername.setText(savedUser.getUsername());
            txtPassword.setText(savedUser.getPassword());
            chkRememberMe.setSelected(true);
        }

        comboRole.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateUI(newVal);
        });
        
        updateUI(comboRole.getValue());

        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> rootPane.requestFocus());
        }
    }
    
    private void updateUI(String role) {
        boolean isCustomer = "Customer".equals(role);
        
        txtUsername.setVisible(!isCustomer);
        txtUsername.setManaged(!isCustomer);
        
        if (isCustomer) {
            txtUsername.setPromptText("Full Name (First & Last)");
            txtUsername.setText(""); 
            txtUsername.setVisible(true);
            txtUsername.setManaged(true);
        } else {
            txtUsername.setPromptText("Username");
            
            User savedUser = userService.getRememberedUser(role);
            if (savedUser != null) {
                txtUsername.setText(savedUser.getUsername());
                txtPassword.setText(savedUser.getPassword());
                chkRememberMe.setSelected(true);
            } else {
                txtUsername.clear();
                txtPassword.clear();
                chkRememberMe.setSelected(false);
            }
        }
        
        txtPhone.setVisible(isCustomer);
        txtPhone.setManaged(isCustomer);
        
        if(txtNationalId != null) {
            txtNationalId.setVisible(isCustomer);
            txtNationalId.setManaged(isCustomer);
        }
        
        boolean showPasswordFields = !isCustomer;
        txtPassword.setVisible(showPasswordFields && !txtVisiblePassword.isVisible());
        txtPassword.setManaged(showPasswordFields);
        txtVisiblePassword.setVisible(showPasswordFields && txtVisiblePassword.isVisible());
        txtVisiblePassword.setManaged(showPasswordFields);
        btnTogglePassword.setVisible(showPasswordFields);
        btnTogglePassword.setManaged(showPasswordFields);
        linkForgotPassword.setVisible(showPasswordFields);
        linkForgotPassword.setManaged(showPasswordFields);
        chkRememberMe.setVisible(showPasswordFields);
        chkRememberMe.setManaged(showPasswordFields);
        
        boolean showHints = false;
        if ("Admin".equals(role) && userService.isDefaultAdmin()) {
            if (lblUserHint != null) lblUserHint.setText("Default Username: admin");
            if (lblPassHint != null) lblPassHint.setText("Default Password: 123");
            showHints = true;
        } else if ("Authorized".equals(role)) {
            if (userService.validateLogin("it_admin", "123456", "Authorized")) {
                if (lblUserHint != null) lblUserHint.setText("Default Username: it_admin");
                if (lblPassHint != null) lblPassHint.setText("Default Password: 123456");
                showHints = true;
            }
        }
        
        if (lblUserHint != null) { lblUserHint.setVisible(showHints); lblUserHint.setManaged(showHints); }
        if (lblPassHint != null) { lblPassHint.setVisible(showHints); lblPassHint.setManaged(showHints); }
        if (lblChangePassHint != null) { lblChangePassHint.setVisible(showHints); lblChangePassHint.setManaged(showHints); }

        if (isCustomer) {
            lblMessage.setText("Enter your Name, National ID, and Phone."); 
            lblMessage.setStyle("-fx-text-fill: #7f8c8d;");
        } else {
            lblMessage.setText("");
        }
    }

    @FXML
    void togglePasswordAction(ActionEvent event) {
        showPassword(!txtVisiblePassword.isVisible());
    }
    
    private void showPassword(boolean show) {
        if (show) {
            txtVisiblePassword.setVisible(true);
            txtVisiblePassword.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            btnTogglePassword.setText("🙈"); 
        } else {
            txtVisiblePassword.setVisible(false);
            txtVisiblePassword.setManaged(false);
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            btnTogglePassword.setText("👁"); 
        }
    }

    @FXML
    void handleForgotPasswordAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Recovery");
        alert.setHeaderText("Forgot your password?");
        alert.setContentText("Please contact IT Support or manually delete 'users.json' to reset to default.");
        alert.showAndWait();
    }

    @FXML
    void btnLoginAction(ActionEvent event) {
        String role = comboRole.getValue();
        if ("Customer".equals(role)) {
            performCustomerLogin();
        } else {
            performStaffLogin(role); 
        }
    }

    private void performStaffLogin(String role) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (userService.validateLogin(user, pass, role)) {
            userService.saveRememberMe(user, pass, role, chkRememberMe.isSelected());
            loadView("main_view", null, role, user);
        } else {
            String hint = userService.getHintForUser(user);
            String errorMsg = "Invalid Username or Password!";
            if (hint != null && !hint.isEmpty()) errorMsg += "\nHint: " + hint;
            showError(errorMsg);
        }
    }

    private void performCustomerLogin() {
        String name = txtUsername.getText().trim(); 
        String nationalId = txtNationalId != null ? txtNationalId.getText().trim() : ""; 
        String phone = txtPhone.getText().trim(); 

        if (name.isEmpty() || nationalId.isEmpty() || phone.isEmpty()) { 
            showError("Please enter your Name, National ID, and Phone Number.");
            return;
        }
        
        if (!name.contains(" ") || name.split("\\s+").length < 2) {
            showError("Please enter your Full Name (First & Last Name).");
            return;
        }
        
        if (!nationalId.matches("^\\d{14}$")) {
            showError("Please enter a valid 14-digit National ID.");
            return;
        }

        if (!phone.matches("^01[0125]\\d{8}$")) {
            showError("Please enter a valid Egyptian phone number.");
            return;
        }

        Customer existingCustomer = customerService.findCustomerByNationalId(nationalId);
        
        if (existingCustomer == null) {
            // ✅ يتم إنشاء العميل الجديد بنوع غير محدد وبدون صورة، ويمكنه تحديثها لاحقاً من بوابة الإدارة
            customerService.addCustomer(name, nationalId, phone, "Not Specified", null);
            existingCustomer = customerService.findCustomerByNationalId(nationalId);
        } else {
            if (!existingCustomer.isActive()) {
                showError("Access Denied: This account has been deactivated. Please contact support.");
                return;
            }
            boolean needsUpdate = false;
            if (!existingCustomer.getName().equalsIgnoreCase(name)) {
                existingCustomer.setName(name);
                needsUpdate = true;
            }
            if (!existingCustomer.getPhone().equals(phone)) {
                existingCustomer.setPhone(phone);
                needsUpdate = true;
            }
            if (needsUpdate) {
                customerService.updateCustomer(existingCustomer);
            }
        }

        loadView("customer_portal", existingCustomer, "Customer", null);
    }

    private void loadView(String fxmlName, Customer customer, String role, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
            Parent view = loader.load();
            
            if ("customer_portal".equals(fxmlName)) {
                CustomerPortalController controller = loader.getController();
                controller.setCustomer(customer);
            } else if ("main_view".equals(fxmlName)) {
                MainController controller = loader.getController();
                controller.initSession(role, username);
            }
            
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();

            Scene scene = new Scene(view, stage.getWidth(), stage.getHeight());
            try {
                scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
            } catch (Exception e) { }

            stage.setScene(scene);
            if (wasMaximized) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading view.");
        }
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #e74c3c;");
    }
}