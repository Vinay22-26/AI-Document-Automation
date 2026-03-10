package com.example.Doc_Automation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_documents")
@Data
public class ProcessedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String documentType;

    @Column(columnDefinition = "LONGTEXT")
    private String extractedContent;

    @Column(columnDefinition = "JSON")
    private String extractedJson;

    private LocalDateTime processedAt = LocalDateTime.now();
}