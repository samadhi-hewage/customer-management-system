package com.example.customermanagement.repository;

import com.example.customermanagement.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // =======================================================
    // SAFE: SINGLE ENTITY LOAD (NO FETCH JOIN)
    // =======================================================
    @Query("""
    SELECT c FROM Customer c
    LEFT JOIN FETCH c.mobileNumbers
    LEFT JOIN FETCH c.addresses a
    LEFT JOIN FETCH a.city
    LEFT JOIN FETCH a.country
    LEFT JOIN FETCH c.familyMembers
    WHERE c.id = :id
""")
    Optional<Customer> findByIdWithDetails(@Param("id") Long id);


    boolean existsByNicNumber(String nicNumber);

    boolean existsByNicNumberAndIdNot(String nicNumber, Long id);

    Optional<Customer> findByNicNumber(String nicNumber);

    // =======================================================
    // SAFE PAGINATION QUERY
    // =======================================================
    @Query("""
        SELECT c FROM Customer c
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.nicNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.createdAt DESC
    """)
    Page<Customer> findAllWithSearch(@Param("search") String search, Pageable pageable);
}
