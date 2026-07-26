package com.carapp.car_rental_and_sales_system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image; // ✅ استيراد كلاس الصورة
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // 🛑 (شغل السطر ده مرة واحدة واعمل Run، وبعدين امسحه أو اعمله تعليق)
        // new com.carapp.car_rental_and_sales_system.util.DataSeeder().run();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("login_view.fxml"));
        scene = new Scene(loader.load()); // لا نحدد حجماً ثابتاً هنا، سنعتمد على الشاشة
        
        try {
            scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        } catch (Exception e) { }

        // إضافة اللوجو لأيقونة البرنامج (في الشريط وفي الـ Taskbar)
        try {
            // تأكد أن الصورة logo.jpg موجودة بجانب ملفات الـ FXML في resources
            Image icon = new Image(App.class.getResourceAsStream("logo.jpg"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Logo not found for icon.");
        }

        // تعديل اسم البرنامج في شريط العنوان
        stage.setTitle("Car System");
        
        stage.setScene(scene);
        
        //  ضبط الأبعاد بناء على الشاشة المتاحة (Visual Bounds)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());

        // تفعيل وضع التكبير بعد ضبط الأبعاد
        stage.setMaximized(true);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}