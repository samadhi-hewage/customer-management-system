package com.example.customermanagement.service;

import com.example.customermanagement.entity.BulkJob;
import com.example.customermanagement.repository.BulkJobRepository;
import com.example.customermanagement.repository.CustomerRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ============================================================
//  BulkUploadServiceTest.java
//  Location: src/test/java/com/example/customermanagement/service/
//
//  Tests the BulkUploadService using:
//    - Mockito mocks for repositories and JdbcTemplate
//    - Programmatically generated Excel files (no real file needed)
//    - MockMultipartFile to simulate HTTP file uploads
//
//  Run: mvn test -Dtest=BulkUploadServiceTest
// ============================================================

@ExtendWith(MockitoExtension.class)
class BulkUploadServiceTest {

    @Mock private BulkJobRepository  bulkJobRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private JdbcTemplate       jdbcTemplate;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    // ─────────────────────────────────────────────────────────
    // Helper — builds an in-memory .xlsx file with given rows
    // Returns it as a byte[] ready to wrap in MockMultipartFile
    //
    // Row 0 = header: Name | Date of Birth | NIC Number
    // Row 1..n = data rows from the provided String[][] array
    // ─────────────────────────────────────────────────────────
    private byte[] buildExcelBytes(String[][] dataRows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Customers");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Date of Birth");
            header.createCell(2).setCellValue("NIC Number");

            // Data rows
            for (int i = 0; i < dataRows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < dataRows[i].length; j++) {
                    row.createCell(j).setCellValue(dataRows[i][j]);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helper — wraps byte[] into a MockMultipartFile
    // ─────────────────────────────────────────────────────────
    private MockMultipartFile toMultipartFile(byte[] bytes, String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(bytes)
        );
    }

    // ================================================================
    //  initiateUpload() tests
    // ================================================================
    @Nested
    @DisplayName("initiateUpload()")
    class InitiateUploadTests {

        @Test
        @DisplayName("should return a non-null UUID job ID")
        void initiateUpload_returnsJobId() throws Exception {
            // ARRANGE
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera", "1990-03-15", "900751234V" }
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            when(bulkJobRepository.save(any(BulkJob.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ACT
            String jobId = bulkUploadService.initiateUpload(file);

            // ASSERT
            assertThat(jobId)
                    .isNotNull()
                    .isNotBlank()
                    .hasSize(36); // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        }

        @Test
        @DisplayName("should save a job with PENDING status immediately")
        void initiateUpload_savesJobWithPendingStatus() throws Exception {
            // ARRANGE
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera", "1990-03-15", "900751234V" }
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "customers.xlsx");

            when(bulkJobRepository.save(any(BulkJob.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // ACT
            bulkUploadService.initiateUpload(file);

            // ASSERT — verify a job was saved with PENDING status
            verify(bulkJobRepository, times(1)).save(argThat(job ->
                    job.getStatus() == BulkJob.JobStatus.PENDING &&
                            job.getFileName().equals("customers.xlsx") &&
                            job.getId() != null
            ));
        }
    }

    // ================================================================
    //  processFileAsync() — the core SAX streaming logic
    // ================================================================
    @Nested
    @DisplayName("processFileAsync()")
    class ProcessFileAsyncTests {

        @Test
        @DisplayName("should insert new customers using batchUpdate")
        void processFile_newCustomers_insertsViaBatch() throws Exception {
            // ARRANGE — 3 rows with unique NICs
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera",   "1990-03-15", "900751234V" },
                    { "Dilani Silva",   "1985-07-22", "857031456V" },
                    { "Ruwan Fernando", "1995-11-08", "952231789V" },
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            // All NICs are new — no existing records
            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            bulkUploadService.processFileAsync("test-job-id", file);

            // ASSERT — batchUpdate was called at least once (flush at end)
            verify(jdbcTemplate, atLeastOnce())
                    .batchUpdate(contains("INSERT"), anyList());
        }

        @Test
        @DisplayName("should update existing customers using jdbcTemplate.update()")
        void processFile_existingNic_updatesInsteadOfInsert() throws Exception {
            // ARRANGE — NIC already exists in DB
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Updated", "1990-03-15", "900751234V" },
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            // NIC already exists → should UPDATE not INSERT
            when(customerRepository.existsByNicNumber("900751234V")).thenReturn(true);
            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            bulkUploadService.processFileAsync("test-job-id", file);

            // ASSERT — update was called, batchUpdate was NOT called for inserts
            verify(jdbcTemplate, atLeastOnce())
                    .update(contains("UPDATE"), any(), any(), any());
        }

        @Test
        @DisplayName("should skip rows with missing Name")
        void processFile_missingName_skipsRow() throws Exception {
            // ARRANGE — row with blank name
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "",              "1990-03-15", "900751234V" },  // missing name → skip
                    { "Dilani Silva",  "1985-07-22", "857031456V" },  // valid
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT — should not throw even with bad row
            assertThatCode(() -> bulkUploadService.processFileAsync("job-id", file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip rows with missing NIC")
        void processFile_missingNic_skipsRow() throws Exception {
            // ARRANGE
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera", "1990-03-15", "" },             // missing NIC → skip
                    { "Dilani Silva", "1985-07-22", "857031456V" },   // valid
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> bulkUploadService.processFileAsync("job-id", file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip rows with invalid date format")
        void processFile_invalidDate_skipsRow() throws Exception {
            // ARRANGE — date in wrong format
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera", "15/03/1990", "900751234V" },   // wrong format → skip
                    { "Dilani Silva", "1985-07-22", "857031456V" },   // valid
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> bulkUploadService.processFileAsync("job-id", file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle empty Excel file (header only) without error")
        void processFile_emptyFile_noRowsProcessed() throws Exception {
            // ARRANGE — only header row, no data
            byte[] excelBytes = buildExcelBytes(new String[0][]);
            MockMultipartFile file = toMultipartFile(excelBytes, "empty.xlsx");

            when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT + ASSERT — should complete without throwing
            assertThatCode(() -> bulkUploadService.processFileAsync("job-id", file))
                    .doesNotThrowAnyException();

            // No inserts or updates should have been called
            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
            verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("should mark job as DONE on success")
        void processFile_success_marksJobDone() throws Exception {
            // ARRANGE
            byte[] excelBytes = buildExcelBytes(new String[][]{
                    { "Ashan Perera", "1990-03-15", "900751234V" },
            });
            MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

            BulkJob job = BulkJob.builder()
                    .id("job-123")
                    .status(BulkJob.JobStatus.PROCESSING)
                    .build();

            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(bulkJobRepository.findById("job-123")).thenReturn(Optional.of(job));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            bulkUploadService.processFileAsync("job-123", file);

            // ASSERT — the job was saved with DONE status at the end
            verify(bulkJobRepository, atLeastOnce()).save(argThat(j ->
                    j.getStatus() == BulkJob.JobStatus.DONE
            ));
        }

        @Test
        @DisplayName("should mark job as FAILED when file is corrupt")
        void processFile_corruptFile_marksJobFailed() {
            // ARRANGE — pass random bytes that are not a valid Excel file
            MockMultipartFile corruptFile = new MockMultipartFile(
                    "file", "corrupt.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new ByteArrayInputStream("not an excel file".getBytes())
            );

            BulkJob job = BulkJob.builder()
                    .id("job-corrupt")
                    .status(BulkJob.JobStatus.PROCESSING)
                    .build();

            when(bulkJobRepository.findById("job-corrupt")).thenReturn(Optional.of(job));
            when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            bulkUploadService.processFileAsync("job-corrupt", corruptFile);

            // ASSERT — job should be marked as FAILED with an error message
            verify(bulkJobRepository, atLeastOnce()).save(argThat(j ->
                    j.getStatus() == BulkJob.JobStatus.FAILED &&
                            j.getErrorMsg() != null
            ));
        }
    }

    // ================================================================
    //  BulkSheetHandler inner class tests
    //  Tests the SAX handler directly without going through the file
    // ================================================================
    @Nested
    @DisplayName("BulkSheetHandler (SAX handler)")
    class BulkSheetHandlerTests {

        private BulkUploadService.BulkSheetHandler handler;

        @BeforeEach
        void setUp() {
            handler = new BulkUploadService.BulkSheetHandler(
                    "test-job", jdbcTemplate, customerRepository, bulkJobRepository
            );
        }

        @Test
        @DisplayName("should skip the header row and not count it")
        void handler_skipsHeaderRow() {
            // ACT — simulate SAX firing for the header row (row 0)
            handler.startRow(0);
            handler.cell("A0", "Name", null);
            handler.cell("B0", "Date of Birth", null);
            handler.cell("C0", "NIC Number", null);
            handler.endRow(0);

            // ASSERT — header should be skipped, total stays 0
            assertThat(handler.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("should count a valid row as total=1 success=1")
        void handler_validRow_countsSuccess() {
            // ARRANGE
            when(customerRepository.existsByNicNumber("900751234V")).thenReturn(false);

            // ACT — skip header first
            handler.startRow(0);
            handler.cell("A0", "Name", null);
            handler.endRow(0);

            // Then process a valid data row
            handler.startRow(1);
            handler.cell("A1", "Ashan Perera", null);
            handler.cell("B1", "1990-03-15", null);
            handler.cell("C1", "900751234V", null);
            handler.endRow(1);

            handler.flushRemaining();

            // ASSERT
            assertThat(handler.getTotal()).isEqualTo(1);
            assertThat(handler.getFailCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should count a row with missing name as failed")
        void handler_missingName_countsFail() {
            // skip header
            handler.startRow(0);
            handler.cell("A0", "Name", null);
            handler.endRow(0);

            // data row with no name cell
            handler.startRow(1);
            // A1 intentionally omitted
            handler.cell("B1", "1990-03-15", null);
            handler.cell("C1", "900751234V", null);
            handler.endRow(1);

            assertThat(handler.getTotal()).isEqualTo(1);
            assertThat(handler.getFailCount()).isEqualTo(1);
            assertThat(handler.getSuccessCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should count a row with bad date as failed")
        void handler_badDate_countsFail() {
            // skip header
            handler.startRow(0);
            handler.endRow(0);

            // data row with wrong date format
            handler.startRow(1);
            handler.cell("A1", "Ashan Perera", null);
            handler.cell("B1", "15-03-1990", null);  // wrong format
            handler.cell("C1", "900751234V", null);
            handler.endRow(1);

            assertThat(handler.getFailCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("flushRemaining() should not throw when batch is empty")
        void handler_flushRemaining_emptyBatch_doesNotThrow() {
            assertThatCode(() -> handler.flushRemaining())
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    //  getJobStatus() tests
    // ================================================================
    @Nested
    @DisplayName("getJobStatus()")
    class GetJobStatusTests {

        @Test
        @DisplayName("should return job when found")
        void getJobStatus_found_returnsJob() {
            // ARRANGE
            BulkJob job = BulkJob.builder()
                    .id("job-abc")
                    .status(BulkJob.JobStatus.PROCESSING)
                    .processed(500)
                    .totalRows(1000)
                    .build();

            when(bulkJobRepository.findById("job-abc")).thenReturn(Optional.of(job));

            // ACT
            BulkJob result = bulkUploadService.getJobStatus("job-abc");

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("job-abc");
            assertThat(result.getStatus()).isEqualTo(BulkJob.JobStatus.PROCESSING);
            assertThat(result.getProcessed()).isEqualTo(500);
        }

        @Test
        @DisplayName("should throw exception when job not found")
        void getJobStatus_notFound_throwsException() {
            when(bulkJobRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bulkUploadService.getJobStatus("bad-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bad-id");
        }
    }
}