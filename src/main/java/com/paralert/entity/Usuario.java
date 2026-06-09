package com.paralert.entity;

import com.paralert.entity.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String nombres;

    @Column(length = 150)
    private String apellidos;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(length = 20)
    private String telefono;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verificado = false;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "nivel_confianza", nullable = false)
    @Builder.Default
    private Integer nivelConfianza = 20;

    @Column(name = "cantidad_advertencias")
    @Builder.Default
    private Integer cantidadAdvertencias = 0;

    @Column(name = "alertas_habilitadas", nullable = false)
    @Builder.Default
    private Boolean alertasHabilitadas = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();
}
