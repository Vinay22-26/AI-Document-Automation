package com.example.Doc_Automation.repository;

import com.example.Doc_Automation.model.ProcessedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<ProcessedDocument, Long> {
}