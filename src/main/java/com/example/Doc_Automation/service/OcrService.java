package com.example.Doc_Automation.service;

import java.io.File;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;


@Service
public class OcrService {

    public String extractText(File imageFile) {

        ITesseract instance = new Tesseract();

        instance.setDatapath("tessdata");

        instance.setLanguage("eng");

        try {

            String result = instance.doOCR(imageFile);
            return result;
        } catch (TesseractException e) {
            System.err.println("OCR Failed: " + e.getMessage());
            return "";
        }
    }
}
