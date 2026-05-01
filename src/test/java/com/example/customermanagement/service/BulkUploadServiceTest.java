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

@ExtendWith(MockitoExtension.class)
class BulkUploadServiceTest {

    @Mock private BulkJobRepository  bulkJobRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private JdbcTemplate       jdbcTemplate;

    @InjectMocks
    private BulkUploadService bulkUploadService;

    private byte[] buildExcelBytes(String[][] dataRows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Customers");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name");
            header.createCell(1).setCellValue("Date of Birth");
            header.createCell(2).setCellValue("NIC Number");

            for (int i = 0; i < dataRows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < dataRows[i].length; j++) {
                    row.createCell(j).setCellValue(dataRows[i][j]);
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Excel file", e);
        }
    }


    private MockMultipartFile toMultipartFile(byte[] bytes, String filename) {
        try {
            return new MockMultipartFile(
                    "file",
                    filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new ByteArrayInputStream(bytes)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    // ===================== initiateUpload =====================

    @Test
    void initiateUpload_returnsJobId() throws Exception {
        byte[] excelBytes = buildExcelBytes(new String[][]{
                { "Ashan Perera", "1990-03-15", "900751234V" }
        });

        MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

        when(bulkJobRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        String jobId = bulkUploadService.initiateUpload(file);

        assertThat(jobId).isNotNull().isNotBlank().hasSize(36);
    }

    // ===================== processFileAsync =====================

    @Test
    void processFile_newCustomers_insertsViaBatch() throws Exception {
        byte[] excelBytes = buildExcelBytes(new String[][]{
                { "Ashan", "1990-03-15", "900751234V" }
        });

        MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

        when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
        when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
        when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bulkUploadService.processFileAsync("job-id", file);

        verify(jdbcTemplate, atLeastOnce())
                .batchUpdate(contains("INSERT"), anyList());
    }

    @Test
    void processFile_existingNic_updates() throws Exception {
        byte[] excelBytes = buildExcelBytes(new String[][]{
                { "Ashan Updated", "1990-03-15", "900751234V" }
        });

        MockMultipartFile file = toMultipartFile(excelBytes, "test.xlsx");

        when(customerRepository.existsByNicNumber("900751234V")).thenReturn(true);
        when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
        when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bulkUploadService.processFileAsync("job-id", file);

        verify(jdbcTemplate, atLeastOnce())
                .update(contains("UPDATE"), any(), any(), any());
    }

    @Test
    void processFile_emptyFile() throws Exception {
        byte[] excelBytes = buildExcelBytes(new String[0][]);
        MockMultipartFile file = toMultipartFile(excelBytes, "empty.xlsx");

        when(bulkJobRepository.findById(anyString())).thenReturn(Optional.of(new BulkJob()));
        when(bulkJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> bulkUploadService.processFileAsync("job-id", file))
                .doesNotThrowAnyException();

        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
    }

    // ===================== getJobStatus =====================

    @Test
    void getJobStatus_found() {
        BulkJob job = BulkJob.builder()
                .id("job-1")
                .status(BulkJob.JobStatus.PROCESSING)
                .build();

        when(bulkJobRepository.findById("job-1")).thenReturn(Optional.of(job));

        BulkJob result = bulkUploadService.getJobStatus("job-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("job-1");
    }

    @Test
    void getJobStatus_notFound() {
        when(bulkJobRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bulkUploadService.getJobStatus("bad-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
