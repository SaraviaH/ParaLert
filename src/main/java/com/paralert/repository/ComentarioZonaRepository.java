package com.paralert.repository;

import com.paralert.entity.ComentarioZona;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComentarioZonaRepository extends JpaRepository<ComentarioZona, Long> {

    List<ComentarioZona> findByZonaOrderByFechaCreacionAsc(ZonaPeligrosa zona);

    @Query("SELECT c FROM ComentarioZona c JOIN FETCH c.usuario WHERE c.zona = :zona ORDER BY c.fechaCreacion ASC")
    List<ComentarioZona> findByZonaWithUsuarioOrderByFechaCreacionAsc(@Param("zona") ZonaPeligrosa zona);

    @Query("SELECT c FROM ComentarioZona c LEFT JOIN FETCH c.zona LEFT JOIN FETCH c.usuario WHERE " +
           "LOWER(c.contenido) LIKE '%falso%' OR " +
           "LOWER(c.contenido) LIKE '%mentira%' OR " +
           "LOWER(c.contenido) LIKE '%broma%' OR " +
           "LOWER(c.contenido) LIKE '%fake%' OR " +
           "LOWER(c.contenido) LIKE '%prueba%' OR " +
           "LOWER(c.contenido) LIKE '%test%' OR " +
           "LOWER(c.contenido) LIKE '%inventado%' OR " +
           "LOWER(c.contenido) LIKE '%ninguno%'")
    List<ComentarioZona> findSuspiciousComments();
    
    void deleteByUsuario(Usuario usuario);
}
