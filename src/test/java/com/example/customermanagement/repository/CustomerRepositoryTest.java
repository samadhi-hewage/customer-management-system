package com.example.customermanagement.repository;

import com.example.customermanagement.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

// ============================================================
//  CustomerRepositoryTest.java
//  Location: src/test/java/com/example/customermanagement/repository/
//
//  Uses @DataJpaTest which:
//    - Starts a real Spring context with JPA only (no web layer)
//    - Replaces MariaDB with H2 in-memory database automatically
//    - Rolls back every test so they don't affect each other
//    - No need to have MariaDB running
//
//  Run: mvn test -Dtest=CustomerRepositoryTest
// ============================================================

@DataJpaTest
@ExtendWith(SpringExtension.class)
class CustomerRepositoryTest {

    // TestEntityManager: lets us save test data directly
    // bypassing the repository — gives us full control in tests
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    // ── Shared test data ──────────────────────────────────────
    private Customer ashan;
    private Customer dilani;
    private Customer ruwan;

    // ─────────────────────────────────────────────────────────
    // Before each test: insert fresh test customers
    // @DataJpaTest rolls back after each test so we always
    // start clean — no leftover data between tests
    // ─────────────────────────────────────────────────────────
    @BeforeEach
    void setUp() {
        // Customer 1
        ashan = new Customer();
        ashan.setName("Ashan Perera");
        ashan.setDateOfBirth(LocalDate.of(1990, 3, 15));
        ashan.setNicNumber("900751234V");
        entityManager.persist(ashan);

        // Customer 2
        dilani = new Customer();
        dilani.setName("Dilani Silva");
        dilani.setDateOfBirth(LocalDate.of(1985, 7, 22));
        dilani.setNicNumber("857031456V");
        entityManager.persist(dilani);

        // Customer 3
        ruwan = new Customer();
        ruwan.setName("Ruwan Fernando");
        ruwan.setDateOfBirth(LocalDate.of(1995, 11, 8));
        ruwan.setNicNumber("952231789V");
        entityManager.persist(ruwan);

        // Flush to make sure entities are in H2 before queries run
        entityManager.flush();
    }

    // ================================================================
    //  findByIdWithDetails() — custom JOIN FETCH query
    // ================================================================
    @Nested
    @DisplayName("findByIdWithDetails()")
    class FindByIdWithDetailsTests {

        @Test
        @DisplayName("should return customer with all details when found")
        void findByIdWithDetails_found_returnsCustomer() {
            // ACT
            Optional<Customer> result = customerRepository.findByIdWithDetails(ashan.getId());

            // ASSERT
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Ashan Perera");
            assertThat(result.get().getNicNumber()).isEqualTo("900751234V");
            assertThat(result.get().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 3, 15));
        }

        @Test
        @DisplayName("should return empty when customer not found")
        void findByIdWithDetails_notFound_returnsEmpty() {
            // ACT
            Optional<Customer> result = customerRepository.findByIdWithDetails(9999L);

            // ASSERT
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should load mobile numbers in the same query")
        void findByIdWithDetails_loadsMobileNumbers() {
            // ARRANGE — add mobile numbers to ashan
            MobileNumber mob = new MobileNumber();
            mob.setNumber("+94771234567");
            mob.setCustomer(ashan);
            entityManager.persist(mob);
            entityManager.flush();

            // ACT
            Optional<Customer> result = customerRepository.findByIdWithDetails(ashan.getId());

            // ASSERT — mobile numbers are loaded (no lazy loading error)
            assertThat(result).isPresent();
            assertThat(result.get().getMobileNumbers())
                    .hasSize(1)
                    .extracting(MobileNumber::getNumber)
                    .containsExactly("+94771234567");
        }

        @Test
        @DisplayName("should load addresses with city and country")
        void findByIdWithDetails_loadsAddresses() {
            // ARRANGE — create country, city, address
            Country country = new Country();
            country.setName("Sri Lanka");
            country.setCode("LK");
            entityManager.persist(country);

            City city = new City();
            city.setName("Colombo");
            city.setCountry(country);
            entityManager.persist(city);

            Address address = new Address();
            address.setAddressLine1("45 Galle Road");
            address.setAddressLine2("Kollupitiya");
            address.setCity(city);
            address.setCountry(country);
            address.setCustomer(ashan);
            entityManager.persist(address);
            entityManager.flush();

            // ACT
            Optional<Customer> result = customerRepository.findByIdWithDetails(ashan.getId());

            // ASSERT
            assertThat(result).isPresent();
            assertThat(result.get().getAddresses()).hasSize(1);
            Address loadedAddr = result.get().getAddresses().iterator().next();

            assertThat(loadedAddr.getAddressLine1()).isEqualTo("45 Galle Road");
            assertThat(loadedAddr.getCity().getName()).isEqualTo("Colombo");
            assertThat(loadedAddr.getCountry().getName()).isEqualTo("Sri Lanka");
        }

        @Test
        @DisplayName("should load family members")
        void findByIdWithDetails_loadsFamilyMembers() {
            // ARRANGE — link ashan and dilani as family
            ashan.getFamilyMembers().add(dilani);
            entityManager.persist(ashan);
            entityManager.flush();

            // ACT
            Optional<Customer> result = customerRepository.findByIdWithDetails(ashan.getId());

            // ASSERT
            assertThat(result).isPresent();
            assertThat(result.get().getFamilyMembers())
                    .hasSize(1)
                    .extracting(Customer::getName)
                    .containsExactly("Dilani Silva");
        }
    }

    // ================================================================
    //  existsByNicNumber()
    // ================================================================
    @Nested
    @DisplayName("existsByNicNumber()")
    class ExistsByNicNumberTests {

        @Test
        @DisplayName("should return true when NIC exists")
        void existsByNicNumber_exists_returnsTrue() {
            boolean result = customerRepository.existsByNicNumber("900751234V");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when NIC does not exist")
        void existsByNicNumber_notExists_returnsFalse() {
            boolean result = customerRepository.existsByNicNumber("000000000X");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should be case sensitive — different case returns false")
        void existsByNicNumber_wrongCase_returnsFalse() {
            // NICs are stored as-is — "900751234v" is different from "900751234V"
            boolean result = customerRepository.existsByNicNumber("900751234v");
            assertThat(result).isFalse();
        }
    }

    // ================================================================
    //  existsByNicNumberAndIdNot()
    // ================================================================
    @Nested
    @DisplayName("existsByNicNumberAndIdNot()")
    class ExistsByNicNumberAndIdNotTests {

        @Test
        @DisplayName("should return false when NIC belongs to the same customer (allow keeping own NIC)")
        void existsByNicNumberAndIdNot_sameCustomer_returnsFalse() {
            // Ashan checking his own NIC during update — should be allowed
            boolean result = customerRepository
                    .existsByNicNumberAndIdNot("900751234V", ashan.getId());
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when NIC belongs to a different customer")
        void existsByNicNumberAndIdNot_differentCustomer_returnsTrue() {
            // Ruwan trying to use Ashan's NIC — should be blocked
            boolean result = customerRepository
                    .existsByNicNumberAndIdNot("900751234V", ruwan.getId());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when NIC does not exist at all")
        void existsByNicNumberAndIdNot_nicNotExists_returnsFalse() {
            boolean result = customerRepository
                    .existsByNicNumberAndIdNot("000000000X", ashan.getId());
            assertThat(result).isFalse();
        }
    }

    // ================================================================
    //  findByNicNumber()
    // ================================================================
    @Nested
    @DisplayName("findByNicNumber()")
    class FindByNicNumberTests {

        @Test
        @DisplayName("should return customer when NIC matches")
        void findByNicNumber_found_returnsCustomer() {
            Optional<Customer> result = customerRepository.findByNicNumber("857031456V");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Dilani Silva");
        }

        @Test
        @DisplayName("should return empty when NIC not found")
        void findByNicNumber_notFound_returnsEmpty() {
            Optional<Customer> result = customerRepository.findByNicNumber("000000000X");
            assertThat(result).isEmpty();
        }
    }

    // ================================================================
    //  findAllWithSearch() — paginated search query
    // ================================================================
    @Nested
    @DisplayName("findAllWithSearch()")
    class FindAllWithSearchTests {

        @Test
        @DisplayName("should return all customers when search is empty")
        void findAllWithSearch_emptySearch_returnsAll() {
            Page<Customer> page = customerRepository
                    .findAllWithSearch("", PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by name (case insensitive)")
        void findAllWithSearch_byName_filtersCorrectly() {
            Page<Customer> page = customerRepository
                    .findAllWithSearch("ashan", PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Ashan Perera");
        }

        @Test
        @DisplayName("should filter by partial name match")
        void findAllWithSearch_partialName_matches() {
            // "Perera" matches both "Ashan Perera" — just one in test data
            Page<Customer> page = customerRepository
                    .findAllWithSearch("Perera", PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by NIC number")
        void findAllWithSearch_byNic_filtersCorrectly() {
            Page<Customer> page = customerRepository
                    .findAllWithSearch("952231789V", PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Ruwan Fernando");
        }

        @Test
        @DisplayName("should return empty page when search matches nobody")
        void findAllWithSearch_noMatch_returnsEmpty() {
            Page<Customer> page = customerRepository
                    .findAllWithSearch("XXXXXXXXXX", PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(0);
            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should respect page size")
        void findAllWithSearch_pageSize_limitsResults() {
            // Request page size of 2 — should only get 2 even though 3 exist
            Page<Customer> page = customerRepository
                    .findAllWithSearch("", PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3); // total is still 3
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return second page correctly")
        void findAllWithSearch_secondPage_returnsRemainingResults() {
            // Page 0 = first 2, Page 1 = remaining 1
            Page<Customer> page = customerRepository
                    .findAllWithSearch("", PageRequest.of(1, 2));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getNumber()).isEqualTo(1); // page index
        }

        @Test
        @DisplayName("should return null search same as empty search")
        void findAllWithSearch_nullSearch_returnsAll() {
            Page<Customer> page = customerRepository
                    .findAllWithSearch(null, PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(3);
        }
    }

    // ================================================================
    //  Standard JpaRepository operations
    // ================================================================
    @Nested
    @DisplayName("Standard JPA operations")
    class StandardJpaTests {

        @Test
        @DisplayName("should save a new customer and assign an ID")
        void save_newCustomer_assignsId() {
            Customer newCustomer = new Customer();
            newCustomer.setName("Priya Rajapaksha");
            newCustomer.setDateOfBirth(LocalDate.of(1992, 4, 30));
            newCustomer.setNicNumber("924211012V");

            Customer saved = customerRepository.save(newCustomer);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Priya Rajapaksha");
        }

        @Test
        @DisplayName("should find all 3 seeded customers")
        void findAll_returnsAllCustomers() {
            assertThat(customerRepository.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("should delete a customer by ID")
        void deleteById_removesCustomer() {
            customerRepository.deleteById(ashan.getId());
            entityManager.flush();

            assertThat(customerRepository.findById(ashan.getId())).isEmpty();
            assertThat(customerRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("should enforce unique NIC constraint")
        void save_duplicateNic_throwsException() {
            Customer duplicate = new Customer();
            duplicate.setName("Another Person");
            duplicate.setDateOfBirth(LocalDate.of(2000, 1, 1));
            duplicate.setNicNumber("900751234V"); // same as ashan's NIC

            // The DB constraint throws when we try to flush the duplicate
            assertThatThrownBy(() -> {
                customerRepository.save(duplicate);
                entityManager.flush(); // forces SQL to execute
            }).isInstanceOf(Exception.class); // DataIntegrityViolationException
        }

        @Test
        @DisplayName("should return correct count")
        void count_returnsCorrectNumber() {
            assertThat(customerRepository.count()).isEqualTo(3);
        }
    }
}