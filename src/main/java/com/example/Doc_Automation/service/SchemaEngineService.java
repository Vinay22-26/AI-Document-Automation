package com.example.Doc_Automation.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service

public class SchemaEngineService {

    private static final Pattern PAN_PATTERN = Pattern.compile(
            "(?i)P\\s*A\\s*N\\s*(?:No\\.?|Number)?\\s*[:\\-]?\\s*([A-Z]{5}[0-9]{4}[A-Z])");

    private static final Pattern GST_PATTERN = Pattern.compile(
            "(?i)(?:GSTIN|GST\\s*No|VAT|Tax\\s*ID)\\s*[:\\-]?\\s*([A-Za-z0-9]{10,15})");


    private static final Pattern INVOICE_PATTERN = Pattern.compile(
            "(?i)(?:Invoice\\s*Number|Invoice\\s*No|Bill\\s*No|Receipt\\s*No|Inv\\s*No|Invoice\\s*#)\\s*[:\\-]?\\s*([A-Za-z0-9\\-\\/]+)");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)(?:Date|Invoice Date)\\s*[:\\-]?\\s*([0-9]{1,2}[\\/\\-][0-9]{1,2}[\\/\\-][0-9]{2,4})");


    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:Grand Total|Net Amount|Total Amount|Amount Payable)\\s*[:\\-]?\\s*(?:Rs\\.?|INR|\\$)?\\s*([0-9,]+\\.?[0-9]*)");

    private String autoDetectIndustry(String text) {
        Map<String, Integer> scoreMap = new HashMap<>();
        scoreMap.put("HEALTHCARE", keywordScore(text, "hospital", "patient", "clinic", "doctor", "medical"));
        scoreMap.put("PHARMA", keywordScore(text, "pharmacy", "medicine", "drug", "gst tax invoice"));
        scoreMap.put("LOGISTICS", keywordScore(text, "lorry", "transport", "lr.no", "container", "consignment"));
        scoreMap.put("FINANCE", keywordScore(text, "vat", "tax id", "finance", "account"));
        scoreMap.put("IT_SERVICES", keywordScore(text, "software", "hosting", "website", "technology", "domain"));

        int maxScore = Collections.max(scoreMap.values());
        if (maxScore == 0) {
            return "GENERAL";
        }

        return scoreMap.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("GENERAL");
    }

    private int keywordScore(String text, String... keywords) {
        int score = 0;
        String lower = text.toLowerCase();
        for (String k : keywords) {
            if (lower.contains(k)) {
                score++;
            }
        }
        return score;
    }

    public Map<String, Object> applySchema(String rawText) {

        Map<String, Object> results = new LinkedHashMap<>();

        if (rawText == null || rawText.isEmpty()) {
            results.put("Error", "Empty document");
            return results;
        }

        String text = normalizeText(rawText);
        String industry = autoDetectIndustry(text);
        results.put("Detected_Schema", industry);


        results.put("Document_Number", extract(INVOICE_PATTERN, text, 1));
        results.put("Tax_ID", extract(GST_PATTERN, text, 1));
        results.put("PAN_Number", extract(PAN_PATTERN, text, 1));
        results.put("Invoice_Date", extract(DATE_PATTERN, text, 1));

        String totalAmount = extract(TOTAL_PATTERN, text, 1);
        if (totalAmount.equals("MISSING")) {
            totalAmount = extractLastTotalFallback(text);
        }
        results.put("Total_Amount", totalAmount);

        switch (industry) {
            case "HEALTHCARE":
                applyHealthcareSchema(results, text);
                break;
            case "LOGISTICS":
                applyLogisticsSchema(results, text);
                break;
            case "PHARMA":
                applyPharmaSchema(results, text);
                break;
            default:
                break;
        }

        results.put("Confidence_Score", calculateConfidence(results));
        return results;
    }

    private void applyHealthcareSchema(Map<String, Object> results, String text) {
        Pattern patientId = Pattern.compile("(?i)Patient\\s*(?:ID|No)?\\s*[:\\-]?\\s*([A-Za-z0-9]+)");
        Pattern patientName = Pattern.compile("(?i)Patient\\s*Name\\s*[:\\-]?\\s*([A-Za-z\\s]+)");

        results.put("Patient_ID", extract(patientId, text, 1));
        results.put("Patient_Name", extract(patientName, text, 1));
    }

    private void applyLogisticsSchema(Map<String, Object> results, String text) {
        Pattern container = Pattern.compile("(?i)Container\\s*(?:ID|No)?\\s*[:\\-]?\\s*([A-Z]{4}\\d{7})");
        Pattern lrNo = Pattern.compile("(?i)LR\\s*No\\.?\\s*[:\\-]?\\s*([A-Za-z0-9]+)");

        results.put("Container_ID", extract(container, text, 1));
        results.put("LR_Number", extract(lrNo, text, 1));
    }

    private void applyPharmaSchema(Map<String, Object> results, String text) {
        Pattern drugLicense = Pattern.compile("(?i)Drug\\s*License\\s*No\\.?\\s*[:\\-]?\\s*([A-Za-z0-9]+)");
        results.put("Drug_License_No", extract(drugLicense, text, 1));
    }

    private String extract(Pattern pattern, String text, int groupIndex) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(groupIndex).trim();
        }
        return "MISSING";
    }

    private String extractLastTotalFallback(String text) {
        Pattern p = Pattern.compile("(?i)Total\\s*[:\\-]?\\s*(?:Rs\\.?|INR|\\$)?\\s*([0-9,]+\\.?[0-9]*)");
        Matcher m = p.matcher(text);
        String lastMatch = "MISSING";
        while (m.find()) {
            lastMatch = m.group(1).trim();
        }
        return lastMatch;
    }

    private String normalizeText(String text) {
        return text
                .replaceAll("[\\r\\n]+", "\n")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("O", "0")
                .replaceAll("I", "1")
                .trim();
    }

    private int calculateConfidence(Map<String, Object> results) {
        int filled = 0;
        int total = results.size() - 2;

        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (!entry.getKey().equals("Detected_Schema") && !entry.getKey().equals("Confidence_Score")) {
                if (entry.getValue() != null && !entry.getValue().toString().equals("MISSING")) {
                    filled++;
                }
            }
        }

        if (total <= 0) return 0;
        return (filled * 100) / total;
    }
}