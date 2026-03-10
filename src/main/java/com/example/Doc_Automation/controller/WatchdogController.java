package com.example.Doc_Automation.controller;


import com.example.Doc_Automation.model.DocumentClassifier;
import com.example.Doc_Automation.model.ProcessedDocument;
import com.example.Doc_Automation.repository.DocumentRepository;
import com.example.Doc_Automation.service.*;
import com.example.Doc_Automation.view.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.File;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WatchdogController {
    private final ExecutorService executor = Executors.newFixedThreadPool(100);

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private UniversalExtractorService reader;

    @Autowired
    private DocumentClassifier classifier;

    @Autowired
    private DataExtractorService dataExtractor;

    @Autowired
    private SchemaEngineService schemaEngineService;

    @Autowired
    private OutputBuilder outputBuilder;

    @Autowired
    private ExportView exporter;

    private final ObjectMapper mapper = new ObjectMapper();

    public void startWatching(String inputDirPath) throws Exception {
        Path path = Paths.get(inputDirPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        // Startup Scan
        File folder = new File(inputDirPath);
        File[] existingFiles = folder.listFiles();
        if (existingFiles != null) {
            for (File f : existingFiles) if (f.isFile()) executor.execute(() -> processFile(f));
        }

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                File file = new File(inputDirPath + "/" + event.context());
                Thread.sleep(800);
                if (file.exists()) executor.execute(() -> processFile(file));
            }
            key.reset();
        }
    }

    private void processFile(File file) {
        String fileName = file.getName();
        try {
            // 1. Extract and Classify
            String text = reader.extract(file);
            DocumentClassifier.DocType type = classifier.classify(text);

            // 2. Extract Data
            Map<String, Object> allData = dataExtractor.extractAllData(file, text);
            Map<String, Object> schemaData = schemaEngineService.applySchema(text);
            Map<String, Object> finalReport = outputBuilder.buildFinalOutput(fileName, type.toString(), allData, schemaData);

            // 3. PERSIST TO MYSQL
            ProcessedDocument dbRecord = new ProcessedDocument();
            dbRecord.setFileName(fileName);
            dbRecord.setDocumentType(type.toString());
            dbRecord.setExtractedContent(text);
            dbRecord.setExtractedJson(mapper.writeValueAsString(finalReport));
            repository.save(dbRecord);

            // 4. Export and Archive
            exporter.exportToJson(finalReport, "exports/" + fileName);
            moveFile(file, "archive");
            AuditLoggerController.log(fileName, "SUCCESS", "SYSTEM");
        } catch (Exception e) {
            AuditLoggerController.log(fileName, "ERROR", "SYSTEM");
            moveFile(file, "errors");
        }
    }

    private synchronized void moveFile(File file, String target) {
        try {
            File dir = new File(target);
            if (!dir.exists()) dir.mkdirs();
            Files.move(file.toPath(), Paths.get(target + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}