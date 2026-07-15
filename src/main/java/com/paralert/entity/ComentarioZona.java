package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "zonas_comentarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentarioZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private ZonaPeligrosa zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    private Integer calificacion; // Calificación por estrellas (1 a 5)

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean sospechoso = false;

    @Column(name = "juicio_tipo", length = 50)
    private String juicioTipo; // FALSO, OFENSIVO, SPAM, PRUEBA, APROBADO

    @Column(name = "juicio_justificacion", columnDefinition = "TEXT")
    private String juicioJustificacion;

    @Column(length = 50)
    private String evaluador; // DICCIONARIO_LOCAL, MODERADO_MANUAL

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;
}

