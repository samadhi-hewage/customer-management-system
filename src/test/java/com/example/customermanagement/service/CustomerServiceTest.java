package com.example.customermanagement.service;

import com.example.customermanagement.dto.CustomerDTO;
import com.example.customermanagement.entity.*;
import com.example.customermanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ============================================================
//  CustomerServiceTest.java
//  Location: src/test/java/com/example/customermanagement/service/
//
//  Tests the CustomerService business logic using Mockito mocks.
//  NO real database is used — repositories are mocked so tests
//  run instantly without needing MariaDB to be running.
//
//  Run all tests: mvn test
//  Run just this file: mvn test -Dtest=CustomerServiceTest
// ============================================================

@ExtendWith(MockitoExtension.class)         // enables Mockito annotations
class CustomerServiceTest {

    // ── Mocks — fake versions of the repositories ────────────
    // Mockito creates these automatically — no real DB needed
    @Mock private CustomerRepository     customerRepository;
    @Mock private MobileNumberRepository mobileNumberRepository;
    @Mock private AddressRepository      addressRepository;
    @Mock private CityRepository         cityRepository;
    @Mock private CountryRepository      countryRepository;

    // ── System under test — real CustomerService with mocked deps
    @InjectMocks
    private CustomerService customerService;

    // ── Shared test data ──────────────────────────────────────
    private Customer sampleCustomer;
    private CustomerDTO sampleDTO;

    @BeforeEach
    void setUp() {
        // Build a sample Customer entity used across tests
        sampleCustomer = new Customer();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("Ashan Perera");
        sampleCustomer.setDateOfBirth(LocalDate.of(1990, 3, 15));
        sampleCustomer.setNicNumber("900751234V");

        // Build a sample DTO (what the controller sends in)
        sampleDTO = CustomerDTO.builder()
                .name("Ashan Perera")
                .dateOfBirth(LocalDate.of(1990, 3, 15))
                .nicNumber("900751234V")
                .mobileNumbers(List.of("+94771234567"))
                .addresses(List.of())
                .familyMembers(List.of())
                .build();
    }

    // ================================================================
    //  CREATE CUSTOMER TESTS
    // ================================================================
    @Nested
    @DisplayName("createCustomer()")
    class CreateCustomerTests {

        @Test
        @DisplayName("should create customer successfully when NIC is unique")
        void createCustomer_success() {
            // ARRANGE — set up what the mocks should return
            when(customerRepository.existsByNicNumber("900751234V"))
                    .thenReturn(false);                          // NIC does not exist yet
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(inv -> {
                        Customer c = inv.getArgument(0);
                        c.setId(1L);                             // simulate DB assigning an ID
                        return c;
                    });

            // ACT — call the real method
            CustomerDTO result = customerService.createCustomer(sampleDTO);

            // ASSERT — verify the result is correct
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Ashan Perera");
            assertThat(result.getNicNumber()).isEqualTo("900751234V");
            assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 3, 15));

            // Verify the repository was actually called once
            verify(customerRepository, times(1)).existsByNicNumber("900751234V");
            verify(customerRepository, times(1)).save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw exception when NIC already exists")
        void createCustomer_duplicateNic_throwsException() {
            // ARRANGE — NIC already exists in DB
            when(customerRepository.existsByNicNumber("900751234V"))
                    .thenReturn(true);

            // ACT + ASSERT — expect an exception
            assertThatThrownBy(() -> customerService.createCustomer(sampleDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("900751234V");

            // Verify save was NEVER called — we should fail before saving
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should save mobile numbers when provided")
        void createCustomer_withMobileNumbers_savesMobiles() {
            // ARRANGE
            sampleDTO.setMobileNumbers(List.of("+94771234567", "+94711234567"));
            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(inv -> {
                        Customer c = inv.getArgument(0);
                        c.setId(1L);
                        return c;
                    });

            // ACT
            CustomerDTO result = customerService.createCustomer(sampleDTO);

            // ASSERT — both mobile numbers should be in the response
            assertThat(result.getMobileNumbers())
                    .hasSize(2)
                    .contains("+94771234567", "+94711234567");
        }

        @Test
        @DisplayName("should create customer with empty mobile numbers list")
        void createCustomer_noMobileNumbers_succeeds() {
            // ARRANGE
            sampleDTO.setMobileNumbers(List.of());
            when(customerRepository.existsByNicNumber(anyString())).thenReturn(false);
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(inv -> { Customer c = inv.getArgument(0); c.setId(1L); return c; });

            // ACT
            CustomerDTO result = customerService.createCustomer(sampleDTO);

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getMobileNumbers()).isEmpty();
        }
    }

    // ================================================================
    //  UPDATE CUSTOMER TESTS
    // ================================================================
    @Nested
    @DisplayName("updateCustomer()")
    class UpdateCustomerTests {

        @Test
        @DisplayName("should update customer successfully")
        void updateCustomer_success() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.existsByNicNumberAndIdNot("900751234V", 1L))
                    .thenReturn(false);
            when(customerRepository.save(any(Customer.class)))
                    .thenReturn(sampleCustomer);

            CustomerDTO updateDTO = CustomerDTO.builder()
                    .name("Ashan Updated")
                    .dateOfBirth(LocalDate.of(1990, 3, 15))
                    .nicNumber("900751234V")
                    .mobileNumbers(List.of("+94779999999"))
                    .addresses(List.of())
                    .familyMembers(List.of())
                    .build();

            // ACT
            CustomerDTO result = customerService.updateCustomer(1L, updateDTO);

            // ASSERT
            assertThat(result).isNotNull();
            verify(customerRepository, times(1)).save(any(Customer.class));
            verify(mobileNumberRepository, times(1)).deleteByCustomerId(1L);
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void updateCustomer_notFound_throwsException() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(99L))
                    .thenReturn(Optional.empty());

            // ACT + ASSERT
            assertThatThrownBy(() -> customerService.updateCustomer(99L, sampleDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when NIC belongs to a different customer")
        void updateCustomer_nicTakenByOther_throwsException() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.existsByNicNumberAndIdNot("900751234V", 1L))
                    .thenReturn(true);         // NIC is taken by someone else

            // ACT + ASSERT
            assertThatThrownBy(() -> customerService.updateCustomer(1L, sampleDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("900751234V");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should delete old addresses before saving new ones")
        void updateCustomer_replacesAddresses() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.existsByNicNumberAndIdNot(anyString(), anyLong()))
                    .thenReturn(false);
            when(customerRepository.save(any())).thenReturn(sampleCustomer);

            // ACT
            customerService.updateCustomer(1L, sampleDTO);

            // ASSERT — old addresses must be deleted before new ones are saved
            verify(addressRepository, times(1)).deleteByCustomerId(1L);
        }
    }

    // ================================================================
    //  GET CUSTOMER TESTS
    // ================================================================
    @Nested
    @DisplayName("getCustomer()")
    class GetCustomerTests {

        @Test
        @DisplayName("should return customer when found")
        void getCustomer_found_returnsDTO() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(sampleCustomer));

            // ACT
            CustomerDTO result = customerService.getCustomer(1L);

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Ashan Perera");
            assertThat(result.getNicNumber()).isEqualTo("900751234V");
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void getCustomer_notFound_throwsException() {
            // ARRANGE
            when(customerRepository.findByIdWithDetails(999L))
                    .thenReturn(Optional.empty());

            // ACT + ASSERT
            assertThatThrownBy(() -> customerService.getCustomer(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }
    }

    // ================================================================
    //  DELETE CUSTOMER TESTS
    // ================================================================
    @Nested
    @DisplayName("deleteCustomer()")
    class DeleteCustomerTests {

        @Test
        @DisplayName("should delete customer when found")
        void deleteCustomer_success() {
            // ARRANGE
            when(customerRepository.existsById(1L)).thenReturn(true);
            doNothing().when(customerRepository).deleteById(1L);

            // ACT — should not throw
            assertThatCode(() -> customerService.deleteCustomer(1L))
                    .doesNotThrowAnyException();

            // ASSERT
            verify(customerRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("should throw exception when customer to delete not found")
        void deleteCustomer_notFound_throwsException() {
            // ARRANGE
            when(customerRepository.existsById(99L)).thenReturn(false);

            // ACT + ASSERT
            assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");

            verify(customerRepository, never()).deleteById(any());
        }
    }

    // ================================================================
    //  MAP ENTITY TO DTO TESTS
    // ================================================================
    @Nested
    @DisplayName("mapEntityToDto()")
    class MapEntityToDtoTests {

        @Test
        @DisplayName("should correctly map all basic fields")
        void mapEntityToDto_mapsCorrectly() {
            // ARRANGE — entity with all fields set
            Customer customer = new Customer();
            customer.setId(5L);
            customer.setName("Dilani Silva");
            customer.setDateOfBirth(LocalDate.of(1985, 7, 22));
            customer.setNicNumber("857031456V");

            // ACT
            CustomerDTO dto = customerService.mapEntityToDto(customer);

            // ASSERT
            assertThat(dto.getId()).isEqualTo(5L);
            assertThat(dto.getName()).isEqualTo("Dilani Silva");
            assertThat(dto.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 7, 22));
            assertThat(dto.getNicNumber()).isEqualTo("857031456V");
            assertThat(dto.getMobileNumbers()).isEmpty();
            assertThat(dto.getAddresses()).isEmpty();
            assertThat(dto.getFamilyMembers()).isEmpty();
        }

        @Test
        @DisplayName("should map mobile numbers to list of strings")
        void mapEntityToDto_mapsMobileNumbers() {
            // ARRANGE
            MobileNumber mob1 = new MobileNumber();
            mob1.setNumber("+94771234567");
            MobileNumber mob2 = new MobileNumber();
            mob2.setNumber("+94711234567");

            sampleCustomer.getMobileNumbers().add(mob1);
            sampleCustomer.getMobileNumbers().add(mob2);

            // ACT
            CustomerDTO dto = customerService.mapEntityToDto(sampleCustomer);

            // ASSERT
            assertThat(dto.getMobileNumbers())
                    .hasSize(2)
                    .containsExactlyInAnyOrder("+94771234567", "+94711234567");
        }

        @Test
        @DisplayName("should map address with city and country names")
        void mapEntityToDto_mapsAddressWithCityAndCountry() {
            // ARRANGE
            Country country = new Country();
            country.setId(1);
            country.setName("Sri Lanka");

            City city = new City();
            city.setId(1);
            city.setName("Colombo");
            city.setCountry(country);

            Address address = new Address();
            address.setId(10L);
            address.setAddressLine1("45 Galle Road");
            address.setAddressLine2("Kollupitiya");
            address.setCity(city);
            address.setCountry(country);
            address.setCustomer(sampleCustomer);

            sampleCustomer.getAddresses().add(address);

            // ACT
            CustomerDTO dto = customerService.mapEntityToDto(sampleCustomer);

            // ASSERT
            assertThat(dto.getAddresses()).hasSize(1);
            CustomerDTO.AddressDTO addrDTO = dto.getAddresses().get(0);
            assertThat(addrDTO.getAddressLine1()).isEqualTo("45 Galle Road");
            assertThat(addrDTO.getCityName()).isEqualTo("Colombo");
            assertThat(addrDTO.getCountryName()).isEqualTo("Sri Lanka");
            assertThat(addrDTO.getCityId()).isEqualTo(1);
            assertThat(addrDTO.getCountryId()).isEqualTo(1);
        }

        @Test
        @DisplayName("should map family members with id, name and nic only")
        void mapEntityToDto_mapsFamilyMembers() {
            // ARRANGE — add a family member to the customer
            Customer familyMember = new Customer();
            familyMember.setId(2L);
            familyMember.setName("Dilani Perera");
            familyMember.setNicNumber("857031456V");

            sampleCustomer.getFamilyMembers().add(familyMember);

            // ACT
            CustomerDTO dto = customerService.mapEntityToDto(sampleCustomer);

            // ASSERT — family members only expose id, name, nic (not full details)
            assertThat(dto.getFamilyMembers()).hasSize(1);
            CustomerDTO.FamilyMemberDTO familyDTO = dto.getFamilyMembers().get(0);
            assertThat(familyDTO.getId()).isEqualTo(2L);
            assertThat(familyDTO.getName()).isEqualTo("Dilani Perera");
            assertThat(familyDTO.getNicNumber()).isEqualTo("857031456V");
        }

        @Test
        @DisplayName("should handle null city and country gracefully")
        void mapEntityToDto_nullCityAndCountry_doesNotThrow() {
            // ARRANGE — address with no city or country set
            Address address = new Address();
            address.setId(10L);
            address.setAddressLine1("45 Galle Road");
            address.setCity(null);
            address.setCountry(null);
            address.setCustomer(sampleCustomer);

            sampleCustomer.getAddresses().add(address);

            // ACT + ASSERT — should not throw NullPointerException
            assertThatCode(() -> customerService.mapEntityToDto(sampleCustomer))
                    .doesNotThrowAnyException();

            CustomerDTO dto = customerService.mapEntityToDto(sampleCustomer);
            assertThat(dto.getAddresses().get(0).getCityName()).isNull();
            assertThat(dto.getAddresses().get(0).getCountryName()).isNull();
        }
    }
}