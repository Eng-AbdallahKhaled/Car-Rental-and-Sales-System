package com.carapp.car_rental_and_sales_system.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;

public class ExportUtil {

    public static <T> void exportTableToExcel(TableView<T> table, Window window, String defaultFileName) {
        
        // غيرنا الصيغة لملف إكسيل حقيقي XLSX
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as Excel (.xlsx)");
        fileChooser.setInitialFileName(defaultFileName + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        File file = fileChooser.showSaveDialog(window);
        
        if (file == null) {
            return;
        }

        // إنشاء ملف إكسيل جديد وشيت جواه
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Data Export");

            // ==========================================
            // 🎨 تجهيز الستايلات والألوان (Themes)
            // ==========================================
            
            // 1. ستايل الهيدر (خلفية رمادي غامق جداً + خط ذهبي عريض)
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            byte[] rgbDark = new byte[]{(byte)26, (byte)26, (byte)26}; // #1a1a1a
            headerStyle.setFillForegroundColor(new XSSFColor(rgbDark, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 13);
            byte[] rgbGold = new byte[]{(byte)252, (byte)213, (byte)19}; // #fcd513
            headerFont.setColor(new XSSFColor(rgbGold, null));
            headerStyle.setFont(headerFont);

            // 2. ستايل البيانات (خلفية بيضاء عادية)
            XSSFCellStyle dataStyle1 = workbook.createCellStyle();
            dataStyle1.setAlignment(HorizontalAlignment.CENTER);
            dataStyle1.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(dataStyle1);

            // 3. ستايل البيانات للصفوف المتبادلة (خلفية رمادي فاتح جداً لراحة العين)
            XSSFCellStyle dataStyle2 = workbook.createCellStyle();
            byte[] rgbLightGrey = new byte[]{(byte)242, (byte)242, (byte)242}; // #f2f2f2
            dataStyle2.setFillForegroundColor(new XSSFColor(rgbLightGrey, null));
            dataStyle2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataStyle2.setAlignment(HorizontalAlignment.CENTER);
            dataStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(dataStyle2);


            // ==========================================
            // 📝 كتابة البيانات في الشيت
            // ==========================================
            
            // 1. كتابة صف العناوين (الهيدر)
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); // تكبير ارتفاع صف الهيدر
            int colIndex = 0;
            for (TableColumn<T, ?> column : table.getColumns()) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(column.getText());
                cell.setCellStyle(headerStyle);
            }

            // 2. كتابة صفوف البيانات من الجدول
            int rowIndex = 1;
            for (T rowData : table.getItems()) {
                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(20);
                
                // بنبدل بين اللون الأبيض والرمادي الفاتح حسب رقم الصف
                XSSFCellStyle currentStyle = (rowIndex % 2 == 0) ? dataStyle2 : dataStyle1;

                colIndex = 0;
                for (TableColumn<T, ?> column : table.getColumns()) {
                    Cell cell = row.createCell(colIndex++);
                    Object cellValue = column.getCellData(rowData);
                    String valueStr = (cellValue == null) ? "" : cellValue.toString();
                    
                    // لو القيمة رقمية بنحولها، لو نصية بنسيبها نص
                    try {
                        double num = Double.parseDouble(valueStr.replace("$", "").replace(",", ""));
                        cell.setCellValue(num);
                    } catch (NumberFormatException e) {
                        cell.setCellValue(valueStr);
                    }
                    
                    cell.setCellStyle(currentStyle);
                }
                rowIndex++;
            }

            // 3. تظبيط عرض العواميد أوتوماتيك عشان الكلام ميتقصش
            for (int i = 0; i < table.getColumns().size(); i++) {
                sheet.autoSizeColumn(i);
                // بنزود العرض شوية عشان يبان مريح وشيك
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1200);
            }

            // حفظ الملف على الجهاز
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            System.out.println("✅ تم تصدير شيت الإكسيل بنجاح إلى: " + file.getAbsolutePath());
            
        } catch (Exception e) {
            System.out.println("❌ حصل مشكلة في تصدير الإكسيل: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // دالة مساعدة لعمل إطارات للخلايا (Borders)
    private static void setBorders(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }
}