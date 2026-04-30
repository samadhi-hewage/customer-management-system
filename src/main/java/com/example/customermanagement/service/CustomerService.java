package com.example.customermanagement.service;

import com.example.customermanagement.dto.CustomerDTO;
import com.example.customermanagement.entity.*;
import com.example.customermanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MobileNumberRepository mobileNumberRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    // ===========================================================
    // CREATE
    // ===========================================================
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO dto) {

        if (dto.getNicNumber() != null &&
                customerRepository.existsByNicNumber(dto.getNicNumber())) {
            throw new IllegalArgumentException("NIC already exists");
        }

        Customer customer = new Customer();
        mapDtoToEntity(dto, customer);

        customer = customerRepository.save(customer);

        linkFamilyMembers(customer, dto.getFamilyMembers());

        return mapEntityToDto(customer);
    }

    // ===========================================================
    // UPDATE
    // ===========================================================
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {

        Customer customer = customerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (customerRepository.existsByNicNumberAndIdNot(dto.getNicNumber(), id)) {
            throw new IllegalArgumentException("NIC already in use");
        }

        customer.setName(dto.getName());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setNicNumber(dto.getNicNumber());

        // SAFE RESET (orphanRemoval handles DB cleanup)
        customer.getMobileNumbers().clear();
        customer.getAddresses().clear();
        customer.getFamilyMembers().clear();

        if (dto.getMobileNumbers() != null) {
            dto.getMobileNumbers().forEach(n -> {
                MobileNumber m = new MobileNumber();
                m.setNumber(n);
                customer.addMobileNumber(m);
            });
        }

        if (dto.getAddresses() != null) {
            dto.getAddresses().forEach(a -> {
                Address address = buildAddress(a, customer);
                customer.addAddress(address);
            });
        }

        if (dto.getFamilyMembers() != null) {
            linkFamilyMembers(customer, dto.getFamilyMembers());
        }

        return mapEntityToDto(customerRepository.save(customer));
    }

    // ===========================================================
    // GET ONE
    // ===========================================================
    @Transactional(readOnly = true)
    public CustomerDTO getCustomer(Long id)
{
        Customer customer = customerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        // FORCE initialize lazy collections
        customer.getMobileNumbers().size();
        customer.getAddresses().size();
        customer.getFamilyMembers().size();

        return mapEntityToDto(customer);
    }


    // ===========================================================
    // GET ALL
    // ===========================================================
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllCustomers(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return customerRepository.findAllWithSearch(search, pageable)
                .map(this::mapEntityToDto);
    }

    // ===========================================================
    // DELETE
    // ===========================================================
    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    // ===========================================================
    // HELPERS
    // ===========================================================
    private void mapDtoToEntity(CustomerDTO dto, Customer customer) {
        customer.setName(dto.getName());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setNicNumber(dto.getNicNumber());

        if (dto.getMobileNumbers() != null) {
            dto.getMobileNumbers().forEach(n -> {
                MobileNumber m = new MobileNumber();
                m.setNumber(n);
                customer.addMobileNumber(m);
            });
        }

        if (dto.getAddresses() != null) {
            dto.getAddresses().forEach(a -> {
                Address address = buildAddress(a, customer);
                customer.addAddress(address);
            });
        }
    }

    private Address buildAddress(CustomerDTO.AddressDTO dto, Customer customer) {
        Address address = new Address();
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setCustomer(customer);

        if (dto.getCityId() != null) {
            cityRepository.findById(dto.getCityId())
                    .ifPresent(address::setCity);
        }

        if (dto.getCountryId() != null) {
            countryRepository.findById(dto.getCountryId())
                    .ifPresent(address::setCountry);
        }

        return address;
    }

    private void linkFamilyMembers(Customer customer,
                                   List<CustomerDTO.FamilyMemberDTO> familyDtos) {

        if (familyDtos == null) return;

        familyDtos.forEach(f -> {
            if (f.getId() != null && !f.getId().equals(customer.getId())) {
                customerRepository.findById(f.getId())
                        .ifPresent(customer::addFamilyMember);
            }
        });
    }

    public CustomerDTO mapEntityToDto(Customer customer) {

        List<String> mobiles = customer.getMobileNumbers()
                .stream()
                .map(MobileNumber::getNumber)
                .collect(Collectors.toList());

        List<CustomerDTO.AddressDTO> addresses = customer.getAddresses()
                .stream()
                .map(a -> CustomerDTO.AddressDTO.builder()
                        .id(a.getId())
                        .addressLine1(a.getAddressLine1())
                        .addressLine2(a.getAddressLine2())
                        .cityId(a.getCity() != null ? a.getCity().getId() : null)
                        .countryId(a.getCountry() != null ? a.getCountry().getId() : null)
                        .build())
                .toList();

        List<CustomerDTO.FamilyMemberDTO> family = customer.getFamilyMembers()
                .stream()
                .map(f -> CustomerDTO.FamilyMemberDTO.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .nicNumber(f.getNicNumber())
                        .build())
                .toList();

        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .dateOfBirth(customer.getDateOfBirth())
                .nicNumber(customer.getNicNumber())
                .mobileNumbers(mobiles)
                .addresses(addresses)
                .familyMembers(family)
                .build();
    }
}
