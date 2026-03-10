package com.example.Doc_Automation.controller;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class AuditLoggerController {
    public static void log(String file, String status, String actor) {
        try (FileWriter fw = new FileWriter("audit_trail.csv", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(LocalDateTime.now() + "," + file + "," + status + "," + actor);
        } catch (Exception ignored) {}
    }
}