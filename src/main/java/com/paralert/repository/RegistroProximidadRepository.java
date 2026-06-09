package com.paralert.repository;

import com.paralert.entity.RegistroProximidad;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface RegistroProximidadRepository extends JpaRepository<RegistroProximidad, Long> {
    Optional<RegistroProximidad> findByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    List<RegistroProximidad> findByZona(ZonaPeligrosa zona);
    void deleteByZona(ZonaPeligrosa zona);
}
