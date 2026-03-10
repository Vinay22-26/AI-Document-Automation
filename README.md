# AI-Document-Automation


A robust, multi-threaded document processing engine built with **Spring Boot** and **Java 21**. This system monitors local directories, extracts text and structured data from various formats (PDF, Image, Word, Excel, CSV), and persists the results to a **MySQL** database.

---

## 🏗️ Architecture: MVC Pattern

The project follows the **Model-View-Controller (MVC)** architectural pattern to ensure modularity and scalability.



- **Controller**: `WatchdogController` monitors folder events and orchestrates the workflow.
- **Model**: `ProcessedDocument` represents the database entity; `UniversalExtractor` routes files to appropriate engines.
- **Service**: Dedicated logic for OCR (Tesseract), Regex extraction, and Spreadsheet parsing.
- **View**: `OutputBuilder` and `ExportService` handle JSON formatting and file exports.

---

## 🚀 Features

- **Universal Extraction**: Automatically handles `.pdf`, `.docx`, `.doc`, `.txt`,  and images (`.png`, `.jpg`).
- **AI-Powered OCR**: Uses **Tess4J (Tesseract)** to read scanned documents and images.
- **Intelligent Classification**: Automatically identifies if a document is an **Invoice** or a **Resume**.
- **Regex Data Mining**: High-speed extraction of GST IDs, Emails, Dates, and Bank Details.
- **Database Persistence**: Every processed document is stored in **MySQL** with its raw text and structured JSON.
- **Multi-threaded**: Uses an `ExecutorService` thread pool to process up to 100 documents simultaneously.

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.2.3
- **Database**: MySQL 8.0
- **OCR Engine**: Tesseract (Tess4J)
- **PDF Engine**: Apache PDFBox / Tabula
- **Office Engine**: Apache POI
- **Build Tool**: Maven

---

## ⚙️ Prerequisites

1. **Java 21** installed.
2. **MySQL Server** running.
3. **Tesseract Data**: Download `eng.traineddata` and place it in your local directory (in a same project folder).

---

## 🔌 Setup & Configuration

### 1. Database Setup
Create a database named `set your configurations ` in MySQL:
```sql
CREATE DATABASE your_db_name ;

