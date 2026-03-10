package com.example.Doc_Automation.view;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OutputBuilder {
    public Map<String, Object> buildFinalOutput(String fileName, String docType, Map<String, Object> extractedData, Map<String, Object> schemaData) {
        Map<String, Object> rootResponse = new LinkedHashMap<>();
        rootResponse.put("message", "Document processed successfully");
        rootResponse.put("result", true);

        Map<String, Object> dataObject = new LinkedHashMap<>();
        dataObject.put("fileName", fileName);
        dataObject.put("documentType", docType);

        if (schemaData != null && !schemaData.isEmpty()) {
            dataObject.putAll(schemaData);
        }

        if (extractedData.containsKey("key_fields")) {
            @SuppressWarnings("unchecked")
            Map<String, String> keys = (Map<String, String>) extractedData.get("key_fields");
            dataObject.putAll(keys);
        }

        if (extractedData.containsKey("table_details")) {
            dataObject.put("table details", extractedData.get("table_details"));
        }

        if (extractedData.containsKey("bank_details")) {
            dataObject.put("bankDetails", extractedData.get("bank_details"));
        }

        List<Map<String, Object>> dataArray = new ArrayList<>();
        dataArray.add(dataObject);
        rootResponse.put("data", dataArray);

        return rootResponse;
    }
}