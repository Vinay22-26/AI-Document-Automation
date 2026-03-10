package com.example.Doc_Automation.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;


@Service
public class ExportView {
    private final ObjectMapper mapper;

    public ExportView() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportToJson(Map<String, Object> data, String outputName) {
        try {
            File outputFile = new File(outputName + ".json");
            mapper.writeValue(outputFile, data);
            System.out.println("Export Successful: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Export Failed: " + e.getMessage());
        }
    }
}