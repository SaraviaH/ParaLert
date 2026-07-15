package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "palabras_sospechosas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PalabraSospechosa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String palabra;

    @Column(nullable = false, length = 50)
    private String categoria; // FALSO, OFENSIVO, SPAM, PRUEBA

    @Column(name = "fecha_creado", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreado = LocalDateTime.now();
}
