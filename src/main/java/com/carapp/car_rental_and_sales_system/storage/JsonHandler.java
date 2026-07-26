package com.carapp.car_rental_and_sales_system.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JsonHandler {

    // إعداد Gson مع معالج خاص للتواريخ (LocalDate)
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter()) // إضافة المحول هنا
            .create();

    public static <T> void save(List<T> list, String filePath) {
        try (Writer writer = new FileWriter(filePath)) {
            GSON.toJson(list, writer);
            System.out.println("Saved data to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving file: " + filePath);
        }
    }

    public static <T> List<T> load(String filePath, Class<T> clazz) {
        try (Reader reader = new FileReader(filePath)) {
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> list = GSON.fromJson(reader, listType);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            // في حالة عدم وجود الملف (أول تشغيل)، نرجع قائمة فارغة
            return new ArrayList<>();
        }
    }

    // كلاس داخلي لتحويل التواريخ من وإلى نصوص
    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString()); // يكتب التاريخ مثل "2024-01-01"
            }
        }

        @Override
        public LocalDate read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == com.google.gson.stream.JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString());
            }
        }
    }
}