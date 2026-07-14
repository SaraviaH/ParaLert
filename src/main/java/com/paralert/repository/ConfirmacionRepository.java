package com.paralert.repository;

import com.paralert.entity.Confirmacion;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.Set;

public interface ConfirmacionRepository extends JpaRepository<Confirmacion, Long> {
    Optional<Confirmacion> findByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    boolean existsByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    long countByZona(ZonaPeligrosa zona);

    @Query("SELECT c.zona.id FROM Confirmacion c WHERE c.usuario.id = :usuarioId")
    Set<Long> findZonaIdsConfirmadasPorUsuario(@Param("usuarioId") Long usuarioId);

    void deleteByUsuario(Usuario usuario);
}

