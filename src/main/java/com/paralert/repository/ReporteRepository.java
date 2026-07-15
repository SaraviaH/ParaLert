package com.paralert.repository;

import com.paralert.entity.Reporte;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findByZona(ZonaPeligrosa zona);

    @Query("SELECT r FROM Reporte r LEFT JOIN FETCH r.zona LEFT JOIN FETCH r.usuario WHERE r.sospechoso = true ORDER BY r.fechaCreacion DESC")
    List<Reporte> findSuspiciousReports();

    @Query("SELECT COALESCE(r.tipoPeligro.nombre, 'Sin Categoría'), COUNT(r) FROM Reporte r GROUP BY r.tipoPeligro.nombre")
    List<Object[]> countReportsByTipoPeligro();

    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN EXTRACT(HOUR FROM fecha_creacion) >= 0 AND EXTRACT(HOUR FROM fecha_creacion) < 6 THEN 'Madrugada (00:00 - 06:00)' " +
           "  WHEN EXTRACT(HOUR FROM fecha_creacion) >= 6 AND EXTRACT(HOUR FROM fecha_creacion) < 12 THEN 'Mañana (06:00 - 12:00)' " +
           "  WHEN EXTRACT(HOUR FROM fecha_creacion) >= 12 AND EXTRACT(HOUR FROM fecha_creacion) < 18 THEN 'Tarde (12:00 - 18:00)' " +
           "  ELSE 'Noche (18:00 - 00:00)' " +
           "END AS rango, COUNT(*) " +
           "FROM reportes_incidentes " +
           "GROUP BY rango", nativeQuery = true)
    List<Object[]> countReportsByHoraRango();

    void deleteByUsuario(Usuario usuario);
}
