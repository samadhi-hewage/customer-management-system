package com.example.customermanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// -------------------------------------------------------
// DTO = Data Transfer Object
// We never expose entities directly to the frontend.
// DTOs control exactly what goes in and out of the API.
// -------------------------------------------------------

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {

    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotNull(message = "Date of birth is mandatory")
    private LocalDate dateOfBirth;

    @NotBlank(message = "NIC number is mandatory")
    private String nicNumber;

    @Builder.Default
    private List<String> mobileNumbers = new ArrayList<>();

    @Builder.Default
    @Valid
    private List<AddressDTO> addresses = new ArrayList<>();

    // Just the IDs of family members — avoids infinite recursion
    @Builder.Default
    private List<FamilyMemberDTO> familyMembers = new ArrayList<>();

    // -------------------------------------------------------
    // Nested DTOs — kept inside CustomerDTO file for simplicity
    // -------------------------------------------------------

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddressDTO {
        private Long id;

        @NotBlank(message = "Address line 1 is mandatory")
        private String addressLine1;

        private String addressLine2;
        private Integer cityId;
        private String  cityName;
        private Integer countryId;
        private String  countryName;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FamilyMemberDTO {
        private Long   id;
        private String name;
        private String nicNumber;
    }
}