package com.example.Doc_Automation.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service

public class SchemaEngineService {

    private static final Pattern PAN_PATTERN = Pattern.compile(
            "(?i)\\b[pP1l][\\s\\.\\-]*[aA@4][\\s\\.\\-]*[nN]\\b\\s*(?:no\\.?|num(?:b[e3]r)?)?\\s*[^A-Z0-9]{0,15}([A-Z]{5}[0-9]{4}[A-Z])");

    private static final Pattern GST_PATTERN = Pattern.compile(
            "(?i)\\b(?:[gG6][sS5][tT7][iI1l]?[nN]?|" +
                    "[gG6][sS5][tT7]\\s*(?:no\\.?|num(?:b[e3]r)?)?|" +
                    "vat|tax\\s*id)\\b" +
                    "\\s*[^A-Z0-9]{0,15}" +
                    "([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z])"
    );
    private static final Pattern INVOICE_PATTERN = Pattern.compile(
            "(?i)\\b(?:"
                    + "invoice|inv\\.?|tax\\s*invoice|commercial\\s*invoice|proforma\\s*invoice|"
                    + "bill|bill\\s*of\\s*supply|statement|statement\\s*of\\s*account|"
                    + "receipt|payment\\s*receipt|sales\\s*receipt|"
                    + "order|order\\s*id|order\\s*number|purchase\\s*order|po|po\\s*number|"
                    + "reference|ref\\.?|transaction|txn|voucher|document|doc"
                    + ")"
                    + "\\s*(?:no\\.?|number|num\\.?|#|id|code)?"
                    + "\\b"
                    + "[^A-Z0-9]{0,20}"
                    + "((?=.*\\d)[A-Z0-9][A-Z0-9\\-\\/\\._]{2,60})"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)\\b(?:"
                    + "date|invoice\\s*date|bill\\s*date|receipt\\s*date|"
                    + "order\\s*date|purchase\\s*date|transaction\\s*date|"
                    + "issued\\s*on|issue\\s*date|posting\\s*date|"
                    + "document\\s*date|doc\\.?\\s*date|dt|d[a@]te"
                    + ")\\b"
                    + "[^0-9A-Z]{0,20}"
                    + "("
                    + "(?:[0-3]?\\d[\\/\\-.][0-1]?\\d[\\/\\-.](?:\\d{2}|\\d{4}))"
                    + "|"
                    + "(?:\\d{4}[\\/\\-.][0-1]?\\d[\\/\\-.][0-3]?\\d)"
                    + "|"
                    + "(?:[0-3]?\\d\\s*(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)"
                    + "[a-z]*[\\s,.-]*\\d{2,4})"
                    + "|"
                    + "(?:(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)"
                    + "[a-z]*\\s*[0-3]?\\d[\\s,.-]*\\d{2,4})"
                    + "|"
                    + "(?:\\d{8})"
                    + ")"
    );

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:g[rn]a?nd\\s*t[o0]tal|n[e3]t\\s*am[o0]unt|t[o0]tal\\s*am[o0]unt|am[o0]unt\\s*payable|t[o0]tal)\\b"
                    + "\\s*[^0-9A-Z]{0,20}"
                    + "(?:rs\\.?|inr|usd|eur|gbp|cad|aud|\\$|€|£|₹)?"
                    + "\\s*"
                    + "([0-9]{1,3}(?:[,\\.][0-9]{3})*(?:[\\.,][0-9]{2})?|[0-9]+(?:[\\.,][0-9]{2})?)"
    );

    private String autoDetectIndustry(String text) {
        Map<String, Integer> scoreMap = new HashMap<>();
        scoreMap.put("RESTAURANT",keywordScore(text,"restaurant", "swiggy", "food", "order id","zomato","blink it","zepto","bigbasket"));
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
        Pattern patientId = Pattern.compile(
                "(?i)\\b[pP][aA@4][tT7][iI1l][eE3]?[nN][tT7]\\s*" +
                        "(?:id|no\\.?|num(?:b[e3]r)?)\\b" +
                        "\\s*[:\\-]?\\s*" +
                        "([A-Za-z0-9\\-\\/]+)"
        );
        Pattern patientName = Pattern.compile(
                "(?i)\\b[pP][aA@4][tT7][iI1l][eE3]?[nN][tT7]\\s*[nN][aA@4][mM][eE3]\\b" +
                        "\\s*[:\\-]?\\s*" +
                        "([A-Za-z\\s\\.]{2,100})(?=\\r?\\n|$)"
        );

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
        Pattern drugLicense = Pattern.compile(
                "(?i)\\b[dD][rR][uUvV][gG9]\\b\\s*[lL1iI][iI1l][cC][eE3][nN][sS5][eE3]\\b\\s*(?:no\\.?|num(?:b[e3]r)?)?\\s*[^A-Z0-9]{0,15}([A-Za-z0-9\\-\\/]+)"
        );
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

        Pattern p = Pattern.compile(
                "(?i)\\b[tT][oO0][tT7][aA@4][lL1iI]\\b" +
                        "\\s*[^0-9A-Z]{0,20}" +
                        "(?:rs\\.?|inr|usd|eur|gbp|cad|aud|\\$|€|£|₹)?" +
                        "\\s*" +
                        "([0-9]{1,3}(?:[,\\.][0-9]{3})*(?:[\\.,][0-9]{2})?|[0-9]+(?:[\\.,][0-9]{2})?)"
        );

        Matcher m = p.matcher(text);
        String lastMatch = "MISSING";

        while (m.find()) {
            lastMatch = m.group(1).trim();
        }

        return lastMatch;
    }

    private String normalizeText(String text) {

        if (text == null) return "";

        return text
                .replaceAll("[\\r\\t]+", " ")
                .replaceAll("\\n{2,}", "\n")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("[–—]", "-")
                .replaceAll("[“”]", "\"")
                .replaceAll("[‘’]", "'")
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