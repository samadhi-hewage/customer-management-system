package com.example.customermanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Date of birth is mandatory")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank(message = "NIC number is mandatory")
    @Column(name = "nic_number", nullable = false, unique = true)
    private String nicNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    // One customer → many mobile numbers
    // CascadeType.ALL  = save/delete mobiles when customer is saved/deleted
    // orphanRemoval    = delete a mobile if removed from the list
    // -------------------------------------------------------
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<MobileNumber> mobileNumbers = new HashSet<>();

    // -------------------------------------------------------
    // One customer → many addresses
    // -------------------------------------------------------
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();

    // -------------------------------------------------------
    // Self-referencing many-to-many for family members
    // A customer can have many family members who are also customers
    // -------------------------------------------------------
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "family_members",
            joinColumns        = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "family_member_id")
    )
    @Builder.Default
    private Set<Customer> familyMembers = new HashSet<>();

    // -------------------------------------------------------
    // Helper methods — keep relationships consistent
    // -------------------------------------------------------
    public void addMobileNumber(MobileNumber mobileNumber) {
        mobileNumbers.add(mobileNumber);
        mobileNumber.setCustomer(this);
    }

    public void removeMobileNumber(MobileNumber mobileNumber) {
        mobileNumbers.remove(mobileNumber);
        mobileNumber.setCustomer(null);
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setCustomer(null);
    }

    public void addFamilyMember(Customer member) {
        this.familyMembers.add(member);
        member.getFamilyMembers().add(this); // keep it two-way
    }

    public void removeFamilyMember(Customer member) {
        this.familyMembers.remove(member);
        member.getFamilyMembers().remove(this);
    }
}