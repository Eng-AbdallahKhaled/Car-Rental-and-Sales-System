package com.carapp.car_rental_and_sales_system.controller;

import com.carapp.car_rental_and_sales_system.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ChangePasswordController {

    @FXML private TextField txtNewUser;
    @FXML private PasswordField txtNewPass;
    @FXML private TextField txtVisiblePass;
    @FXML private TextField txtHint;
    @FXML private Button btnToggle;

    private UserService userService;
    private String currentUsername;
    // Callback لتحديث اسم المستخدم في الواجهة الرئيسية لو تغير
    private Runnable onUpdateSuccess; 

    public void initData(UserService userService, String currentUsername, String currentHint) {
        this.userService = userService;
        this.currentUsername = currentUsername;
        
        txtNewUser.setText(currentUsername);
        txtHint.setText(currentHint);
        
        // ربط الحقول
        txtVisiblePass.textProperty().bindBidirectional(txtNewPass.textProperty());
    }
    
    public void setOnUpdateSuccess(Runnable onUpdateSuccess) {
        this.onUpdateSuccess = onUpdateSuccess;
    }

    @FXML
    void togglePass(ActionEvent event) {
        if (txtVisiblePass.isVisible()) {
            txtVisiblePass.setVisible(false);
            txtVisiblePass.setManaged(false);
            txtNewPass.setVisible(true);
            txtNewPass.setManaged(true);
            btnToggle.setText("👁");
        } else {
            txtVisiblePass.setVisible(true);
            txtVisiblePass.setManaged(true);
            txtNewPass.setVisible(false);
            txtNewPass.setManaged(false);
            btnToggle.setText("🙈");
        }
    }

    @FXML
    void closeAction(ActionEvent event) {
        ((Stage) txtNewUser.getScene().getWindow()).close();
    }

    @FXML
    void saveAction(ActionEvent event) {
        String newUser = txtNewUser.getText().trim();
        String newPass = txtNewPass.getText().trim();
        String newHint = txtHint.getText().trim();

        if (newUser.isEmpty() || newPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Username and Password cannot be empty.");
            return;
        }

        // تحديث البيانات
        userService.updateCredentials(currentUsername, newUser, newPass, newHint);
        
        showAlert(Alert.AlertType.INFORMATION, "Success", "Credentials updated successfully!");
        
        // تشغيل الـ callback لتحديث الواجهة الرئيسية إذا لزم الأمر
        if (onUpdateSuccess != null) onUpdateSuccess.run();
        
        closeAction(null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}