package com.example.Doc_Automation.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

@Service

public class DataExtractorService {
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b"
    );
    private static final Pattern DATE = Pattern.compile(
            "(?i)\\b(" +
                    "(?:0?[1-9]|[12][0-9]|3[01])[\\/\\-.](?:0?[1-9]|1[0-2])[\\/\\-.](?:19|20)\\d{2}" +
                    "|" +
                    "(?:19|20)\\d{2}[\\/\\-.](?:0?[1-9]|1[0-2])[\\/\\-.](?:0?[1-9]|[12][0-9]|3[01])" +
                    "|" +
                    "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s\\d{1,2},?\\s(?:19|20)\\d{2}" +
                    "|" +
                    "\\d{1,2}\\s(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s(?:19|20)\\d{2}" +
                    ")\\b"
    );
    private static final Pattern MONEY = Pattern.compile(
            "(?i)\\b(?:\\$|₹|€|£|rs\\.?|inr|usd|eur|gbp|cad|aud)?\\s?" +
                    "(?:[0-9]{1,3}(?:[,\\.][0-9]{3})*|[0-9]+)" +
                    "(?:[\\.,][0-9]{2})?\\b"
    );

    public Map<String, Object> extractAllData(File file, String text) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("full_content", (text != null) ? text.trim() : "");

        Map<String, String> keyFields = new HashMap<>();
        keyFields.put("email", find(text, EMAIL));
        keyFields.put("date", find(text, DATE));
        keyFields.put("identified_total", findTotal(text));
        data.put("key_fields", keyFields);

        Map<String, String> bankDetails = new LinkedHashMap<>();
        bankDetails.put("Bank_Name",
                extractGroup(text,
                        "(?si)\\b(?:b[a@4]nk\\s*d[e3]tails|b[a@4]nk\\s*n[a@4]me|b[a@4]nk)\\b" +
                                "\\s*[:\\-]?\\s*\\r?\\n?\\s*" +
                                "([A-Za-z\\s&.,'-]{3,100}?(?:Bank|International|Corporation|Limited|Ltd|Inc|Group|Trust)[^\\n\\r]*)"
                )
        );
        bankDetails.put("Account_Number",
                extractGroup(text,
                        "(?i)\\b(?:" +
                                "a\\s*c\\s*c\\s*o\\s*u\\s*n\\s*t\\s*(?:n\\s*u\\s*m\\s*b\\s*e\\s*r|n\\s*o\\.?)?" +
                                "|" +
                                "a\\s*/?\\s*c\\s*\\.?\\s*n\\s*o\\.?" +
                                "|" +
                                "a\\s*c\\s*c\\s*t\\s*n\\s*o\\.?" +
                                ")" +
                                "\\s*[:\\-]?\\s*" +
                                "([A-Z0-9][A-Z0-9\\s\\-]{5,33})"
                )
        );
        bankDetails.put("IFSC_Code",
                extractGroup(text,
                        "(?i)\\b[iI1l][fF][sS5][cC]\\b(?:\\s*code)?\\s*[:\\-]?\\s*" +
                                "([A-Z]{4}0[A-Z0-9]{6})"
                )
        );
        data.put("bank_details", bankDetails);

        if (file != null && file.getName().toLowerCase().endsWith(".pdf")) {
            List<Map<String, String>> tabulaData = extractPdfTableWithTabula(file);
            if (tabulaData.isEmpty()) {
                data.put("table_details", extractTableDetailsAdvanced(text));
            } else {
                data.put("table_details", tabulaData);
            }
        } else {
            data.put("table_details", extractTableDetailsAdvanced(text));
        }

        return data;
    }

    private String find(String t, Pattern p) {
        if (t == null) return "Not Found";
        Matcher m = p.matcher(t);
        return m.find() ? m.group() : "Not Found";
    }

    private String extractGroup(String t, String regex) {
        if (t == null) return "Not Found";
        Matcher m = Pattern.compile(regex).matcher(t);
        return m.find() ? m.group(1).trim() : "Not Found";
    }

    private String findTotal(String t) {

        if (t == null) return "Not Found";

        Pattern p = Pattern.compile(
                "(?i)\\b(" +
                        "n[e3]t\\s*am[o0]unt|" +
                        "sub\\s*t[o0]tal|" +
                        "t[o0]tal\\s*am[o0]unt|" +
                        "t[o0]tal|" +
                        "balance|" +
                        "g[rn]a?nd\\s*t[o0]tal" +
                        ")\\b" +
                        "[:\\s\\-]*" +
                        "(" +
                        "(?:\\$|₹|€|£|rs\\.?|inr|usd|eur|gbp|cad|aud)?\\s*" +
                        "(?:[0-9]{1,3}(?:[,\\.][0-9]{3})*|[0-9]+)" +
                        "(?:[\\.,][0-9]{2})?" +
                        ")"
        );

        Matcher m = p.matcher(t);
        if (m.find()) return m.group(2).trim();

        Matcher m2 = MONEY.matcher(t);
        double max = 0;
        String res = "Not Found";

        while (m2.find()) {
            try {
                String clean = m2.group().replaceAll("[^0-9.]", "");
                double v = Double.parseDouble(clean);
                if (v > max) {
                    max = v;
                    res = m2.group();
                }
            } catch (NumberFormatException ignored) {}
        }

        return res;
    }

    @SuppressWarnings("rawtypes")
    private List<Map<String, String>> extractPdfTableWithTabula(File file) {
        List<Map<String, String>> finalTable = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
            ObjectExtractor oe = new ObjectExtractor(document);
            PageIterator pi = oe.extract();
            while (pi.hasNext()) {
                Page page = pi.next();
                List<Table> tables = bea.extract(page);
                for (Table table : tables) {
                    List<List<RectangularTextContainer>> rows = table.getRows();
                    if (rows == null || rows.size() < 2) continue;

                    boolean isValidTable = false;
                    for (RectangularTextContainer cell : rows.get(0)) {
                        String txt = cell.getText().toLowerCase();
                        if (txt.matches(".*(qty|quantity|description|price|amount|item|particular|hsn).*")) {
                            isValidTable = true;
                            break;
                        }
                    }
                    if (!isValidTable) continue;

                    List<String> headers = new ArrayList<>();
                    for (RectangularTextContainer cell : rows.get(0)) {
                        String headerText = cell.getText().replace("\r", " ").replace("\n", " ").trim();
                        headers.add(headerText.isEmpty() ? "Col_" + headers.size() : headerText);
                    }

                    for (int i = 1; i < rows.size(); i++) {
                        Map<String, String> rowMap = new LinkedHashMap<>();
                        List<RectangularTextContainer> row = rows.get(i);
                        boolean hasData = false;
                        for (int j = 0; j < row.size(); j++) {
                            String header = (j < headers.size()) ? headers.get(j).replaceAll("[^a-zA-Z0-9% ]", "").trim().replace(" ", "_") : "Col_" + j;
                            String cellData = row.get(j).getText().replace("\r", " ").replace("\n", " ").trim();
                            if (!cellData.isEmpty()) hasData = true;
                            rowMap.put(header, cellData);
                        }
                        if (hasData) {
                            finalTable.add(rowMap);
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return finalTable;
    }

    private List<Map<String, String>> extractTableDetailsAdvanced(String text) {
        List<Map<String, String>> tableData = new ArrayList<>();
        if (text == null || text.isEmpty()) return tableData;

        text = text.replaceAll("(?i)Item\\s+Description", "Item_Description")
                .replaceAll("(?i)Unit\\s+Price", "Unit_Price")
                .replaceAll("(?i)Product\\s+Name", "Product_Name")
                .replaceAll("(?i)Net\\s+Amount", "Net_Amount");

        String[] lines = text.split("\\r?\\n");
        boolean inTable = false;
        List<String> headers = new ArrayList<>();

        for (String line : lines) {
            String lower = line.toLowerCase();

            if (!inTable) {
                if (lower.contains("qty") || lower.contains("description") || lower.contains("amount") || lower.contains("product") || lower.contains("rate") || lower.contains("hsn")) {
                    inTable = true;
                    String[] parts = line.split("(\\s{2,}|\\t|\\|)");
                    if (parts.length < 2) parts = line.split("\\s+");
                    for (String p : parts) {
                        String clean = p.replaceAll("[^a-zA-Z0-9%_ ]", "").trim();
                        if (!clean.isEmpty()) headers.add(clean.replace(" ", "_"));
                    }
                    if (headers.isEmpty()) {
                        headers.addAll(List.of("Item", "Description", "Qty", "Price", "Total"));
                    }
                    continue;
                }
            }

            if (inTable) {
                if (lower.contains("subtotal") || lower.matches(".*\\btotal\\b\\s*:?.*") || lower.contains("bank details") || lower.contains("balance") || lower.contains("net amount") || lower.contains("tax amt") || lower.contains("remarks")) {
                    break;
                }

                String cleanLine = line.replaceAll("^\\|", "").trim();
                if (cleanLine.length() < 3) continue;

                Matcher m = Pattern.compile("^(.*?)\\s+((?:[\\$S\\p{Sc}]?\\s?\\d+(?:[\\.,]\\d+)?\\s*)+)$").matcher(cleanLine);

                if (m.find()) {
                    String leftPart = m.group(1).trim();
                    String rightNumbers = m.group(2).trim();

                    String itemNo = "";
                    String description = leftPart;
                    Matcher leftM = Pattern.compile("^(\\d+[\\.,]?)\\s+(.*)$").matcher(leftPart);
                    if (leftM.find()) {
                        itemNo = leftM.group(1).trim();
                        description = leftM.group(2).trim();
                    }

                    String[] numbers = rightNumbers.split("\\s+");
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    int hIdx = 0;

                    if (!itemNo.isEmpty()) {
                        rowMap.put(hIdx < headers.size() ? headers.get(hIdx++) : "Col_" + hIdx, itemNo);
                    }
                    if (!description.isEmpty()) {
                        rowMap.put(hIdx < headers.size() ? headers.get(hIdx++) : "Col_" + hIdx, description);
                    }
                    for (String num : numbers) {
                        rowMap.put(hIdx < headers.size() ? headers.get(hIdx++) : "Col_" + hIdx, num.replace("S", "$"));
                    }
                    tableData.add(rowMap);

                } else {
                    String[] parts = cleanLine.split("(\\s{2,}|\\t|\\|)");
                    if (parts.length < 2) parts = cleanLine.split("\\s+");

                    if (parts.length >= 2) {
                        Map<String, String> rowMap = new LinkedHashMap<>();
                        for (int j = 0; j < parts.length; j++) {
                            String h = j < headers.size() ? headers.get(j) : "Col_" + j;
                            rowMap.put(h, parts[j].trim());
                        }
                        tableData.add(rowMap);
                    }
                }
            }
        }
        return tableData;
    }
}