package com.carapp.car_rental_and_sales_system.util;

import com.carapp.car_rental_and_sales_system.model.Car;
import com.carapp.car_rental_and_sales_system.model.Customer;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class InvoiceGenerator {

    private static final String INVOICE_ROOT = "invoices";
    
    // الألوان الجديدة المتوافقة مع الطباعة (خلفية بيضاء)
    private static final BaseColor COLOR_DARK_GOLD = new BaseColor(184, 134, 11); // ذهبي غامق ليكون مقروءاً على الأبيض
    private static final BaseColor COLOR_TEXT = BaseColor.BLACK; // لون النص الأساسي
    private static final BaseColor COLOR_GREEN = new BaseColor(39, 174, 96);
    private static final BaseColor COLOR_RED = new BaseColor(231, 76, 60);
    private static final BaseColor COLOR_LIGHT_GREY = new BaseColor(240, 240, 240); // لخلفية الجداول
    private static final BaseColor COLOR_BORDER = BaseColor.GRAY; // لون إطارات الجدول

    // الخطوط المحدثة
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, COLOR_DARK_GOLD);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_DARK_GOLD);
    private static final Font TERMS_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_RED);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_TEXT);
    private static final Font DATA_FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_TEXT);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_DARK_GOLD);

    public static void generateSaleInvoice(String saleId, Car car, Customer customer, double price) {
        String folderName = "Sales";
        String safeCarName = (car.getBrand() + "_" + car.getModel()).replaceAll("\\s+", "_");
        String safeCustName = customer.getName().replaceAll("\\s+", "_");
        String fileName = saleId + "_Sale_" + safeCarName + "_" + safeCustName + ".pdf";
        generatePDF(folderName, fileName, "SALE INVOICE", saleId, car, customer, price, null, null);
    }

    public static void generateRentalInvoice(String rentalId, Car car, Customer customer, double totalPrice, LocalDate start, LocalDate end) {
        String folderName = "Rentals";
        String safeCarName = (car.getBrand() + "_" + car.getModel()).replaceAll("\\s+", "_");
        String safeCustName = customer.getName().replaceAll("\\s+", "_");
        String fileName = rentalId + "_Rental_" + safeCarName + "_" + safeCustName + ".pdf";
        generatePDF(folderName, fileName, "RENTAL CONTRACT", rentalId, car, customer, totalPrice, start, end);
    }

    public static void generateReturnReceipt(String rentalId, Car car, Customer customer) {
        String folderName = "Returns";
        String safeCarName = (car.getBrand() + "_" + car.getModel()).replaceAll("\\s+", "_");
        String safeCustName = customer.getName().replaceAll("\\s+", "_");
        String fileName = rentalId + "_Return_" + safeCarName + "_" + safeCustName + ".pdf";
        generatePDF(folderName, fileName, "RENTAL RETURN RECEIPT", rentalId, car, customer, 0, null, null);
    }

    public static void generateSaleRefundReceipt(String saleId, Car car, Customer customer, double refundedAmount) {
        String folderName = "Refunds";
        String safeCarName = (car.getBrand() + "_" + car.getModel()).replaceAll("\\s+", "_");
        String safeCustName = customer.getName().replaceAll("\\s+", "_");
        String fileName = saleId + "_Refund_" + safeCarName + "_" + safeCustName + ".pdf";
        generatePDF(folderName, fileName, "REFUND RECEIPT", saleId, car, customer, refundedAmount, null, null);
    }

    private static void generatePDF(String folderName, String fileName, String title, String id, Car car, Customer customer, double price, LocalDate start, LocalDate end) {
        try {
            File rootDir = new File(INVOICE_ROOT);
            if (!rootDir.exists()) rootDir.mkdir();
            File subDir = new File(rootDir, folderName);
            if (!subDir.exists()) subDir.mkdirs();

            File pdfFile = new File(subDir, fileName);
            // إزالة تلوين الخلفية لتصبح بيضاء بشكل افتراضي
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

            document.open();

            Paragraph header = new Paragraph(title, TITLE_FONT);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            document.add(new Paragraph("\n"));
            addText(document, "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            addText(document, "Transaction ID: " + id);
            addSeparator(document);

            document.add(new Paragraph("\nCUSTOMER DETAILS:", HEADER_FONT));
            addText(document, "Name: " + customer.getName());
            addText(document, "National ID: " + (customer.getNationalId() != null ? customer.getNationalId() : "N/A")); 
            addText(document, "Phone: " + customer.getPhone());
            
            document.add(new Paragraph("\nCAR DETAILS:", HEADER_FONT));
            addText(document, "Brand: " + car.getBrand());
            addText(document, "Model: " + car.getModel());
            addText(document, "Year: " + car.getYear());
            addText(document, "Color: " + car.getColor());

            document.add(new Paragraph("\nPAYMENT & STATUS:", HEADER_FONT));
            document.add(new Paragraph("\n"));
            
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            
            if (start != null && end != null) {
                addTableRow(table, "Rental Start Date", start.toString(), false);
                addTableRow(table, "Rental End Date", end.toString(), false);
                addTableRow(table, "Duration", java.time.temporal.ChronoUnit.DAYS.between(start, end) + " Days", false);
            }
            
            if (title.contains("RETURN")) {
                addTableRow(table, "Status", "Car Returned Successfully", true); 
            } else if (title.equals("REFUND RECEIPT")) {
                addTableRow(table, "Status", "Sale Refunded Successfully", true);
                NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                addTableRow(table, "Refunded Amount", currency.format(price), false);
            } else {
                NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
                addTableRow(table, "Total Amount", currency.format(price), false);
            }
            document.add(table);
            
            if (title.contains("RENTAL CONTRACT")) {
                document.add(new Paragraph("\nTERMS & CONDITIONS:", TERMS_HEADER_FONT));
                addText(document, "- The customer acknowledges receiving the car in good condition.");
                addText(document, "- In case of any damage, the customer is obliged to pay repair compensation.");
            }
            
            document.add(new Paragraph("\n\n\n"));
            
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            PdfPCell cellAuth = new PdfPCell(new Paragraph("Authorized Signature\n\n_______________________", DATA_FONT));
            cellAuth.setBorder(Rectangle.NO_BORDER);
            cellAuth.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell cellCust = new PdfPCell(new Paragraph("Customer Signature\n\n_______________________", DATA_FONT));
            cellCust.setBorder(Rectangle.NO_BORDER);
            cellCust.setHorizontalAlignment(Element.ALIGN_CENTER);
            signatureTable.addCell(cellAuth);
            signatureTable.addCell(cellCust);
            document.add(signatureTable);

            Paragraph footerMsg = new Paragraph("\nThank you for dealing with Luxury Car Showroom!", FOOTER_FONT);
            footerMsg.setAlignment(Element.ALIGN_CENTER);
            document.add(footerMsg);

            // طباعة صور السيارة إذا وجدت
            if (car.getImages() != null && !car.getImages().isEmpty()) {
                document.newPage(); 
                Paragraph imgHeader = new Paragraph("CAR GALLERY", TITLE_FONT);
                imgHeader.setAlignment(Element.ALIGN_CENTER);
                document.add(imgHeader);
                document.add(new Paragraph("\n"));
                for (String imgPath : car.getImages()) {
                    try {
                        Image img = Image.getInstance(imgPath);
                        img.scaleToFit(500, 350);
                        img.setAlignment(Element.ALIGN_CENTER);
                        img.setBorder(Rectangle.BOX);
                        img.setBorderColor(COLOR_BORDER);
                        img.setBorderWidth(1);
                        img.setSpacingAfter(20f);
                        document.add(img);
                    } catch (Exception e) {
                        // تجاهل الصورة المكسورة حتى لا تفسد عملية توليد الـ PDF
                    }
                }
            }
            document.close();
            
            // فتح الملف تلقائياً بعد إنشائه
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            }

        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static void addText(Document doc, String text) throws Exception {
        doc.add(new Paragraph(text, DATA_FONT));
    }
    
    private static void addSeparator(Document doc) throws Exception {
        // الخط الفاصل أصبح رمادياً
        Paragraph p = new Paragraph("--------------------------------------------------------------------------------", FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY));
        doc.add(p);
    }

    private static void addTableRow(PdfPTable table, String header, String value, boolean isSuccess) {
        // عنوان الخلية: نص عريض، خلفية رمادي فاتح، مع إطار رمادي
        PdfPCell cell1 = new PdfPCell(new Phrase(header, DATA_FONT_BOLD));
        cell1.setBackgroundColor(COLOR_LIGHT_GREY);
        cell1.setBorderColor(COLOR_BORDER);
        cell1.setPadding(8);
        table.addCell(cell1);
        
        // قيمة الخلية: خلفية بيضاء، نص أخضر إذا كان نجاح، وإلا نص أسود عادي
        Font valFont = isSuccess ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_GREEN) : DATA_FONT;
        PdfPCell cell2 = new PdfPCell(new Phrase(value, valFont));
        cell2.setBackgroundColor(BaseColor.WHITE);
        cell2.setBorderColor(COLOR_BORDER);
        cell2.setPadding(8);
        table.addCell(cell2);
    }
}