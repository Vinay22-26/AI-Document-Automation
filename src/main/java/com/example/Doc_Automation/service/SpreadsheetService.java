package com.example.Doc_Automation.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;


@Service
public class SpreadsheetService {
    public String readExcel(File file) {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            for (Sheet sheet : workbook) {
                text.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        text.append(cell.toString()).append(" | ");
                    }
                    text.append("\n");
                }
            }
        } catch (Exception e) {
            return "Excel Error: " + e.getMessage();
        }
        return text.toString();
    }

    public String readCsv(File file) {
        StringBuilder text = new StringBuilder();
        try (Reader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            for (CSVRecord csvRecord : csvParser) {
                text.append(csvRecord.toString()).append("\n");
            }
        } catch (IOException e) {
            return "CSV Error: " + e.getMessage();
        }
        return text.toString();
    }
}