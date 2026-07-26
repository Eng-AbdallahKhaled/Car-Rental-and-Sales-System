package com.carapp.car_rental_and_sales_system.service;

import com.carapp.car_rental_and_sales_system.model.User;
import com.carapp.car_rental_and_sales_system.storage.JsonHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    
    private static final String USERS_FILE = "users.json";
    private static final String CONFIG_FILE = "config.json"; 
    
    private List<User> users;

    public UserService() {
        loadUsers();
    }
    
    private void loadUsers() {
        users = JsonHandler.load(USERS_FILE, User.class);
        boolean adminExists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase("admin"));
        boolean itExists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase("it_admin"));
        boolean needSave = false;
        if (!adminExists) { users.add(new User("admin", "123", "Admin")); needSave = true; }
        if (!itExists) { users.add(new User("it_admin", "123456", "Authorized")); needSave = true; }
        if (needSave) saveUsers();
    }
    
    private void saveUsers() { JsonHandler.save(users, USERS_FILE); }

    public boolean validateLogin(String username, String password, String role) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username) && user.getPassword().equals(password) && user.getRole().equalsIgnoreCase(role)) return true;
        }
        return false;
    }
    
    public String getHintForUser(String username) {
        for (User user : users) if (user.getUsername().equalsIgnoreCase(username)) return user.getPasswordHint();
        return null;
    }

    public void updateCredentials(String oldUsername, String newUsername, String newPassword, String newHint) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(oldUsername)) {
                user.setUsername(newUsername);
                user.setPassword(newPassword);
                user.setPasswordHint(newHint);
                saveUsers();
                return;
            }
        }
    }

    public boolean isDefaultAdmin() { return validateLogin("admin", "123", "Admin"); }

    //  حفظ قائمة المستخدمين المتذكرين بدلا من استبدالهم
    public void saveRememberMe(String username, String password, String role, boolean remember) {
        List<User> config = JsonHandler.load(CONFIG_FILE, User.class);
        
        // حذف أي تذكر سابق لنفس الدور (عشان نحدثه بالجديد أو نمسحه)
        config.removeIf(u -> u.getRole().equalsIgnoreCase(role));
        
        if (remember) {
            config.add(new User(username, password, role));
        }
        
        JsonHandler.save(config, CONFIG_FILE);
    }

    //  استرجاع المستخدم بناء على الدور
    public User getRememberedUser(String role) {
        List<User> config = JsonHandler.load(CONFIG_FILE, User.class);
        return config.stream()
                     .filter(u -> u.getRole().equalsIgnoreCase(role))
                     .findFirst()
                     .orElse(null);
    }
    
    // دالة قديمة للتوافق (ترجع أي واحد)
    public User getRememberedUser() {
        List<User> config = JsonHandler.load(CONFIG_FILE, User.class);
        return config.isEmpty() ? null : config.get(0);
    }
}