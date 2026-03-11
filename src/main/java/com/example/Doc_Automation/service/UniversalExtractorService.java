package com.example.Doc_Automation.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.*;
import java.nio.file.Files;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;




@Component
public class UniversalExtractorService {
    @Autowired private OcrService ocrService;
    @Autowired private SpreadsheetService spreadsheetService;

    public String extract(File file) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".docx")) return readDocx(file);
            if (name.endsWith(".doc")) return readDoc(file);
            if (name.endsWith(".pdf")) return readPdf(file);
            if (name.endsWith(".txt")) return new String(Files.readAllBytes(file.toPath()));
            if (name.endsWith(".rtf")) return readRtf(file);
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) return spreadsheetService.readExcel(file);
            if (name.endsWith(".csv")) return spreadsheetService.readCsv(file);
            if (name.matches(".*\\.(png|jpg|jpeg|gif|tif|tiff)$")) return ocrService.extractText(file);
            return "Unsupported Format";
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }

    private String readDocx(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             XWPFDocument d = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(d)) {
            return extractor.getText();
        }
    }

    private String readDoc(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             HWPFDocument d = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(d)) {
            return extractor.getText();
        }
    }

    private String readRtf(File f) throws Exception {
        RTFEditorKit rtf = new RTFEditorKit();
        javax.swing.text.Document d = new DefaultStyledDocument();
        try (FileInputStream fis = new FileInputStream(f)) { rtf.read(fis, d, 0); }
        return d.getText(0, d.getLength());
    }

    private String readPdf(File f) throws IOException {
        try (PDDocument d = Loader.loadPDF(f)) {
            String t = new PDFTextStripper().getText(d);
            return t.trim().isEmpty() ? ocrService.extractText(f) : t;
        }
    }
}