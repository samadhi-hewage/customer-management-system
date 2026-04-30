package com.example.customermanagement.repository;

import com.example.customermanagement.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {

    // -------------------------------------------------------
    // All countries sorted alphabetically
    // Used to populate the country dropdown on the address form
    // -------------------------------------------------------
    List<Country> findAllByOrderByNameAsc();
}