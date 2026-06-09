package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "zonas_peligrosas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonaPeligrosa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_peligro_id", nullable = true)
    private TipoPeligro tipoPeligro;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private String nivelRiesgo; // OBSERVACION, BAJO, MEDIO, ALTO, CRITICO

    @Column(name = "puntaje", nullable = false)
    @Builder.Default
    private Integer puntaje = 10;

    @Column(name = "radio", nullable = false)
    @Builder.Default
    private Integer radio = 10;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "OBSERVACION";

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_ultima_actividad", nullable = false)
    @Builder.Default
    private LocalDateTime fechaUltimaActividad = LocalDateTime.now();

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ComentarioZona> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reporte> reportes = new ArrayList<>();

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Confirmacion> confirmaciones = new ArrayList<>();
}
