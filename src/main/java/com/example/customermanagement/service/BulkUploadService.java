package com.example.customermanagement.service;

import com.example.customermanagement.entity.BulkJob;
import com.example.customermanagement.repository.BulkJobRepository;
import com.example.customermanagement.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;

import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {

    private final BulkJobRepository bulkJobRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;

    // ================= INITIATE UPLOAD =================
    public String initiateUpload(MultipartFile file) {
        String jobId = UUID.randomUUID().toString();

        BulkJob job = BulkJob.builder()
                .id(jobId)
                .fileName(file.getOriginalFilename())
                .status(BulkJob.JobStatus.PENDING)
                .build();

        bulkJobRepository.save(job);

        processFileAsync(jobId, file);

        return jobId;
    }

    // ================= ASYNC PROCESS =================

    @Async
    public void processFileAsync(String jobId, MultipartFile file) {

        bulkJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(BulkJob.JobStatus.PROCESSING);
            bulkJobRepository.save(job);
        });

        try (InputStream is = file.getInputStream();
             OPCPackage pkg = OPCPackage.open(is)) {

            XSSFReader reader = new XSSFReader(pkg);

            StylesTable styles = reader.getStylesTable();

            // ✔ correct import
            org.apache.poi.xssf.model.SharedStringsTable sst =
                    (org.apache.poi.xssf.model.SharedStringsTable)
                            reader.getSharedStringsTable();

            BulkSheetHandler handler = new BulkSheetHandler(
                    jobId, jdbcTemplate, customerRepository, bulkJobRepository
            );

            XMLReader parser = XMLReaderFactory.createXMLReader();

            // ✔ FIXED constructor (THIS IS THE KEY FIX)
            parser.setContentHandler(
                    new XSSFSheetXMLHandler(
                            styles,
                            sst,
                            handler,
                            false
                    )
            );

            XSSFReader.SheetIterator sheets =
                    (XSSFReader.SheetIterator) reader.getSheetsData();

            if (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    parser.parse(new InputSource(sheetStream));
                }
            }

            handler.flushRemaining();

            bulkJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(BulkJob.JobStatus.DONE);
                job.setTotalRows(handler.getTotal());
                job.setProcessed(handler.getSuccessCount());
                job.setFailed(handler.getFailCount());
                bulkJobRepository.save(job);
            });

        } catch (Exception e) {
            log.error("Bulk job failed: {}", e.getMessage(), e);

            bulkJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(BulkJob.JobStatus.FAILED);
                job.setErrorMsg(e.getMessage());
                bulkJobRepository.save(job);
            });
        }
    }

    // ================= STATUS =================
    public BulkJob getJobStatus(String jobId) {
        return bulkJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    // ================= SHEET HANDLER =================
    static class BulkSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final String jobId;
        private final JdbcTemplate jdbcTemplate;
        private final CustomerRepository customerRepository;
        private final BulkJobRepository bulkJobRepository;

        private Map<Integer, String> currentRow;
        private boolean isHeader = true;

        private final List<Object[]> batch = new ArrayList<>();

        private final AtomicInteger total = new AtomicInteger();
        private final AtomicInteger success = new AtomicInteger();
        private final AtomicInteger fail = new AtomicInteger();

        private static final int COL_NAME = 0;
        private static final int COL_DOB = 1;
        private static final int COL_NIC = 2;

        private static final String INSERT_SQL =
                "INSERT IGNORE INTO customers (name, date_of_birth, nic_number) VALUES (?, ?, ?)";

        private static final String UPDATE_SQL =
                "UPDATE customers SET name = ?, date_of_birth = ? WHERE nic_number = ?";

        private static final DateTimeFormatter DATE_FMT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        BulkSheetHandler(String jobId,
                         JdbcTemplate jdbcTemplate,
                         CustomerRepository customerRepository,
                         BulkJobRepository bulkJobRepository) {

            this.jobId = jobId;
            this.jdbcTemplate = jdbcTemplate;
            this.customerRepository = customerRepository;
            this.bulkJobRepository = bulkJobRepository;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new HashMap<>();
        }

        @Override
        public void cell(String cellReference, String value, XSSFComment comment) {
            if (value == null) return;
            int col = cellReferenceToIndex(cellReference);
            currentRow.put(col, value.trim());
        }

        @Override
        public void endRow(int rowNum) {

            if (isHeader) {
                isHeader = false;
                return;
            }

            String name = currentRow.get(COL_NAME);
            String dob = currentRow.get(COL_DOB);
            String nic = currentRow.get(COL_NIC);

            if (name == null || dob == null || nic == null) {
                fail.incrementAndGet();
                total.incrementAndGet();
                return;
            }

            try {
                LocalDate.parse(dob, DATE_FMT);
            } catch (DateTimeParseException e) {
                fail.incrementAndGet();
                total.incrementAndGet();
                return;
            }

            total.incrementAndGet();

            if (customerRepository.existsByNicNumber(nic)) {
                jdbcTemplate.update(UPDATE_SQL, name, dob, nic);
                success.incrementAndGet();
            } else {
                batch.add(new Object[]{name, dob, nic});
                if (batch.size() >= BATCH_SIZE) {
                    flushBatch();
                }
            }

            if (total.get() % 1000 == 0) {
                updateProgress();
            }
        }

        void flushBatch() {
            if (batch.isEmpty()) return;

            try {
                jdbcTemplate.batchUpdate(INSERT_SQL, batch);
                success.addAndGet(batch.size());
            } catch (Exception e) {
                fail.addAndGet(batch.size());
            }

            batch.clear();
        }

        void flushRemaining() {
            flushBatch();
            updateProgress();
        }

        void updateProgress() {
            bulkJobRepository.updateProgress(
                    jobId,
                    success.get(),
                    fail.get(),
                    BulkJob.JobStatus.PROCESSING
            );
        }

        private int cellReferenceToIndex(String ref) {
            int col = 0;
            for (char c : ref.toCharArray()) {
                if (Character.isLetter(c)) {
                    col = col * 26 + (Character.toUpperCase(c) - 'A' + 1);
                } else break;
            }
            return col - 1;
        }

        int getTotal() { return total.get(); }
        int getSuccessCount() { return success.get(); }
        int getFailCount() { return fail.get(); }
    }
}
