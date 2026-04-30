package com.example.customermanagement.controller;

import com.example.customermanagement.entity.BulkJob;
import com.example.customermanagement.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bulk")
@RequiredArgsConstructor
@Slf4j
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    // -------------------------------------------------------
    // POST /api/bulk/upload
    // Accepts an .xlsx file, starts background processing,
    // returns a jobId immediately — no timeout risk
    // -------------------------------------------------------
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        // Basic file validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Please select a file to upload"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Only Excel files (.xlsx, .xls) are accepted"));
        }

        log.info("Bulk upload received: {} ({} bytes)",
                filename, file.getSize());

        String jobId = bulkUploadService.initiateUpload(file);

        Map<String, String> response = new HashMap<>();
        response.put("jobId",   jobId);
        response.put("message", "File upload started. Use jobId to track progress.");
        response.put("status",  "PENDING");

        return ResponseEntity.accepted().body(response);
    }

    // -------------------------------------------------------
    // GET /api/bulk/status/{jobId}
    // Frontend polls this every 2 seconds to show progress bar
    // -------------------------------------------------------
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @PathVariable String jobId) {

        BulkJob job = bulkUploadService.getJobStatus(jobId);

        // Calculate percentage for the frontend progress bar
        int percent = 0;
        if (job.getTotalRows() > 0) {
            percent = (int) ((job.getProcessed() + job.getFailed()) * 100L
                    / job.getTotalRows());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId",      job.getId());
        response.put("status",     job.getStatus().name());
        response.put("fileName",   job.getFileName());
        response.put("totalRows",  job.getTotalRows());
        response.put("processed",  job.getProcessed());
        response.put("failed",     job.getFailed());
        response.put("percent",    percent);
        response.put("errorMsg",   job.getErrorMsg());
        response.put("createdAt",  job.getCreatedAt());
        response.put("updatedAt",  job.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}