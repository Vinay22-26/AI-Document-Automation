package com.example.Doc_Automation.view;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OutputBuilder {

    public Map<String, Object> buildFinalOutput(
            String fileName,
            String docType,
            Map<String, Object> extractedData,
            Map<String, Object> schemaData) {

        Map<String, Object> rootResponse = new LinkedHashMap<>();
        rootResponse.put("message", "Document processed successfully");
        rootResponse.put("result", true);

        Map<String, Object> dataObject = new LinkedHashMap<>();
        dataObject.put("fileName", fileName);
        dataObject.put("documentType", docType);


        if (schemaData != null &&
                "RESTAURANT".equals(schemaData.get("Detected_Schema"))) {

            dataObject = buildRestaurantJson(
                    fileName,
                    docType,
                    extractedData,
                    schemaData
            );

        } else {


            if (schemaData != null && !schemaData.isEmpty()) {
                dataObject.putAll(schemaData);
            }

            if (extractedData.containsKey("key_fields")) {
                @SuppressWarnings("unchecked")
                Map<String, String> keys =
                        (Map<String, String>) extractedData.get("key_fields");
                dataObject.putAll(keys);
            }

            if (extractedData.containsKey("table_details")) {
                dataObject.put("table details",
                        extractedData.get("table_details"));
            }

            if (extractedData.containsKey("bank_details")) {
                dataObject.put("bankDetails",
                        extractedData.get("bank_details"));
            }
        }

        List<Map<String, Object>> dataArray = new ArrayList<>();
        dataArray.add(dataObject);
        rootResponse.put("data", dataArray);

        return rootResponse;
    }



    private Map<String, Object> buildRestaurantJson(
            String fileName,
            String docType,
            Map<String, Object> extractedData,
            Map<String, Object> schemaData) {

        Map<String, Object> restaurantJson = new LinkedHashMap<>();

        restaurantJson.put("platform", detectPlatform(fileName));
        restaurantJson.put("document_type", "RESTAURANT_INVOICE");


        Map<String, Object> orderDetails = new LinkedHashMap<>();
        orderDetails.put("invoice_number",
                schemaData.getOrDefault("Document_Number", "MISSING"));
        orderDetails.put("invoice_date",
                schemaData.getOrDefault("Invoice_Date", "MISSING"));
        restaurantJson.put("order_details", orderDetails);


        Map<String, Object> restaurantDetails = new LinkedHashMap<>();
        restaurantDetails.put("gstin",
                schemaData.getOrDefault("Tax_ID", "MISSING"));
        restaurantDetails.put("pan",
                schemaData.getOrDefault("PAN_Number", "MISSING"));
        restaurantJson.put("restaurant_details", restaurantDetails);


        Map<String, Object> amountSummary = new LinkedHashMap<>();
        amountSummary.put("grand_total",
                schemaData.getOrDefault("Total_Amount", "MISSING"));
        amountSummary.put("currency", "INR");
        restaurantJson.put("amount_summary", amountSummary);


        if (extractedData.containsKey("table_details")) {
            restaurantJson.put("tax_breakup",
                    extractedData.get("table_details"));
        }


        if (extractedData.containsKey("key_fields")) {
            @SuppressWarnings("unchecked")
            Map<String, String> keys =
                    (Map<String, String>) extractedData.get("key_fields");

            restaurantJson.put("additional_info", keys);
        }

        restaurantJson.put("confidence_score",
                schemaData.getOrDefault("Confidence_Score", 0));

        return restaurantJson;
    }


    private String detectPlatform(String fileName) {
        if (fileName == null) return "RESTAURANT";

        String lower = fileName.toLowerCase();

        if (lower.contains("swiggy")) return "SWIGGY";
        if (lower.contains("zomato")) return "ZOMATO";
        if (lower.contains("uber")) return "UBER_EATS";
        if (lower.contains("blinkit")) return "BLINKIT";

        return "RESTAURANT";
    }
}