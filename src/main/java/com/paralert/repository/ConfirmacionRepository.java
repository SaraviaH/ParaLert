package com.paralert.repository;

import com.paralert.entity.Confirmacion;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfirmacionRepository extends JpaRepository<Confirmacion, Long> {
    Optional<Confirmacion> findByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    boolean existsByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    long countByZona(ZonaPeligrosa zona);
}
