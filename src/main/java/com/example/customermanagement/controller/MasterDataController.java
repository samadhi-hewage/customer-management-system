package com.example.customermanagement.controller;

import com.example.customermanagement.entity.City;
import com.example.customermanagement.entity.Country;
import com.example.customermanagement.repository.CityRepository;
import com.example.customermanagement.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// -------------------------------------------------------
// Serves master data to the frontend for dropdowns.
// Countries and cities never change at runtime so we
// keep this controller simple — no service layer needed.
// -------------------------------------------------------
@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterDataController {

    private final CountryRepository countryRepository;
    private final CityRepository    cityRepository;

    // -------------------------------------------------------
    // GET /api/master/countries
    // Returns all countries sorted A-Z for the dropdown
    // -------------------------------------------------------
    @GetMapping("/countries")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(countryRepository.findAllByOrderByNameAsc());
    }

    // -------------------------------------------------------
    // GET /api/master/cities?countryId=1
    // Returns cities filtered by country — called when user
    // picks a country so the city dropdown updates
    // -------------------------------------------------------
    @GetMapping("/cities")
    public ResponseEntity<List<City>> getCitiesByCountry(
            @RequestParam Integer countryId) {
        return ResponseEntity.ok(cityRepository.findByCountryId(countryId));
    }
}