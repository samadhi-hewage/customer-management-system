package com.example.customermanagement.repository;

import com.example.customermanagement.entity.MobileNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MobileNumberRepository extends JpaRepository<MobileNumber, Long> {

    // -------------------------------------------------------
    // Delete all mobile numbers for a customer in one query
    // Used before re-inserting updated numbers on customer edit
    // -------------------------------------------------------
    @Modifying
    @Transactional
    @Query("DELETE FROM MobileNumber m WHERE m.customer.id = :customerId")
    void deleteByCustomerId(@Param("customerId") Long customerId);
}