package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_incidentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private ZonaPeligrosa zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_peligro_id", nullable = false)
    private TipoPeligro tipoPeligro;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

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

