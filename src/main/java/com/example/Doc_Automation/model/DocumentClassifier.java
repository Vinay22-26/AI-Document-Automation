package com.example.Doc_Automation.model;


import org.springframework.stereotype.Service;

@Service
public class DocumentClassifier {


    public enum DocType {
        INVOICE, RESUME, UNKNOWN
    }

    public DocType classify(String text) {
        if (text == null || text.isEmpty()) return DocType.UNKNOWN;

        String lowerText = text.toLowerCase();


        int invoiceScore = countKeywords(lowerText, "invoice", "total", "balance due", "tax", "bill to");
        int resumeScore = countKeywords(lowerText, "resume", "curriculum vitae", "experience", "education", "skills");


        if (invoiceScore > resumeScore && invoiceScore > 0) {
            return DocType.INVOICE;
        } else if (resumeScore > invoiceScore && resumeScore > 0) {
            return DocType.RESUME;
        } else {
            return DocType.UNKNOWN;
        }
    }


    private int countKeywords(String text, String... keywords) {
        int score = 0;
        for (String word : keywords) {
            if (text.contains(word)) {
                score++;
            }
        }
        return score;
    }
}