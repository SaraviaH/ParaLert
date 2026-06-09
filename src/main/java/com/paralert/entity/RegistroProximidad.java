package com.paralert.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_proximidad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "zona_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroProximidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private ZonaPeligrosa zona;

    @Column(name = "fecha_ultima_notificacion", nullable = false)
    private LocalDateTime fechaUltimaNotificacion;
}
