package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_peligros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoPeligro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}
