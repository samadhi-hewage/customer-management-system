package com.example.customermanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Country name is mandatory")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Country code is mandatory")
    @Column(name = "code", nullable = false, length = 3, unique = true)
    private String code;
}