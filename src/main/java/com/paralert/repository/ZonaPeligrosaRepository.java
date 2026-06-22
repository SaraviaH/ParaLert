package com.paralert.repository;

import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;

public interface ZonaPeligrosaRepository extends JpaRepository<ZonaPeligrosa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT z FROM ZonaPeligrosa z WHERE z.id = :id")
    java.util.Optional<ZonaPeligrosa> findByIdForUpdate(@Param("id") Long id);

    List<ZonaPeligrosa> findAllByOrderByFechaCreacionDesc();

    @Query("SELECT DISTINCT z FROM ZonaPeligrosa z " +
           "LEFT JOIN FETCH z.usuario " +
           "LEFT JOIN FETCH z.tipoPeligro " +
           "WHERE z.estado <> 'CERRADA' " +
           "AND z.latitud BETWEEN :minLat AND :maxLat " +
           "AND z.longitud BETWEEN :minLon AND :maxLon " +
           "ORDER BY z.fechaCreacion DESC")
    List<ZonaPeligrosa> findZonesInViewport(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon
    );

    @Query("SELECT DISTINCT z FROM ZonaPeligrosa z " +
           "LEFT JOIN FETCH z.usuario " +
           "LEFT JOIN FETCH z.tipoPeligro " +
           "ORDER BY z.fechaCreacion DESC")
    List<ZonaPeligrosa> findAllWithUsuarioAndTipoPeligro();

    @Query("SELECT z.nivelRiesgo, COUNT(z) FROM ZonaPeligrosa z GROUP BY z.nivelRiesgo")
    List<Object[]> countZonesByNivelRiesgo();

    @Query("SELECT COALESCE(tp.nombre, 'Sin Categoría'), COUNT(z) FROM ZonaPeligrosa z LEFT JOIN z.tipoPeligro tp GROUP BY tp.nombre")
    List<Object[]> countZonesByTipoPeligro();

    @Query("SELECT DISTINCT z FROM ZonaPeligrosa z LEFT JOIN FETCH z.reportes WHERE z.puntaje > 100 OR (SELECT COUNT(r) FROM Reporte r WHERE r.zona = z AND r.fechaCreacion > :limite) >= 2")
    List<ZonaPeligrosa> findZonesWithHighRecentActivity(@Param("limite") java.time.LocalDateTime limite);
}
