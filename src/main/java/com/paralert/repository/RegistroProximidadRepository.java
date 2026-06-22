package com.paralert.repository;

import com.paralert.entity.RegistroProximidad;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface RegistroProximidadRepository extends JpaRepository<RegistroProximidad, Long> {
    Optional<RegistroProximidad> findByUsuarioAndZona(Usuario usuario, ZonaPeligrosa zona);
    List<RegistroProximidad> findByZona(ZonaPeligrosa zona);
    void deleteByZona(ZonaPeligrosa zona);

    @Query("SELECT rp.zona.id, COALESCE(rp.zona.titulo, 'Sin Título'), COALESCE(rp.zona.nivelRiesgo, 'OBSERVACION'), COUNT(rp) " +
           "FROM RegistroProximidad rp " +
           "WHERE rp.zona IS NOT NULL " +
           "GROUP BY rp.zona.id, rp.zona.titulo, rp.zona.nivelRiesgo " +
           "ORDER BY COUNT(rp) DESC")
    List<Object[]> findTopHotZones(Pageable pageable);
}
