package com.example.customermanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkJob {

    // UUID string — generated in the service before saving
    @Id
    @Column(name = "id", length = 36)
    private String id;

    // PENDING → PROCESSING → DONE or FAILED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_rows")
    @Builder.Default
    private int totalRows = 0;

    @Column(name = "processed")
    @Builder.Default
    private int processed = 0;

    @Column(name = "failed")
    @Builder.Default
    private int failed = 0;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    // Status enum — kept inside BulkJob to group related code
    // -------------------------------------------------------
    public enum JobStatus {
        PENDING,
        PROCESSING,
        DONE,
        FAILED
    }
}