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

    @Query("SELECT c FROM ComentarioZona c JOIN FETCH c.usuario WHERE c.zona = :zona AND (c.sospechoso IS NULL OR c.sospechoso = false) ORDER BY c.fechaCreacion ASC")
    List<ComentarioZona> findByZonaWithUsuarioOrderByFechaCreacionAsc(@Param("zona") ZonaPeligrosa zona);

    @Query("SELECT c FROM ComentarioZona c LEFT JOIN FETCH c.zona LEFT JOIN FETCH c.usuario WHERE c.sospechoso = true ORDER BY c.fechaCreacion DESC")
    List<ComentarioZona> findSuspiciousComments();
    
    void deleteByUsuario(Usuario usuario);
}
