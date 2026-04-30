package com.example.customermanagement.repository;

import com.example.customermanagement.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {

    // -------------------------------------------------------
    // Find all cities for a given country
    // Used to populate city dropdown after user selects a country
    // -------------------------------------------------------
    @Query("SELECT c FROM City c WHERE c.country.id = :countryId ORDER BY c.name ASC")
    List<City> findByCountryId(@Param("countryId") Integer countryId);
}