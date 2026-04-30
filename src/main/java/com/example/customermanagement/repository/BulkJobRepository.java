package com.example.customermanagement.repository;

import com.example.customermanagement.entity.BulkJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkJobRepository extends JpaRepository<BulkJob, String> {

    // -------------------------------------------------------
    // Update job progress in one targeted query
    // Avoids loading the full entity just to update 3 numbers
    // -------------------------------------------------------
    @Modifying
    @Query("""
        UPDATE BulkJob j
        SET j.processed = :processed,
            j.failed    = :failed,
            j.status    = :status
        WHERE j.id = :id
    """)
    void updateProgress(
            @Param("id")        String id,
            @Param("processed") int processed,
            @Param("failed")    int failed,
            @Param("status")    BulkJob.JobStatus status
    );
}