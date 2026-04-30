package com.example.customermanagement.repository;

import com.example.customermanagement.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // -------------------------------------------------------
    // Delete all addresses for a customer in one query
    // Used before re-inserting updated addresses on customer edit
    // -------------------------------------------------------
    @Modifying
    @Transactional
    @Query("DELETE FROM Address a WHERE a.customer.id = :customerId")
    void deleteByCustomerId(@Param("customerId") Long customerId);
}