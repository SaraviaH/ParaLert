package com.paralert.repository;

import com.paralert.entity.SolicitudAmistad;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudAmistadRepository extends JpaRepository<SolicitudAmistad, Long> {

    List<SolicitudAmistad> findByReceptorAndEstado(Usuario receptor, EstadoSolicitud estado);

    @Query("SELECT s FROM SolicitudAmistad s JOIN FETCH s.emisor WHERE s.receptor = :receptor AND s.estado = :estado")
    List<SolicitudAmistad> findByReceptorAndEstadoWithEmisor(@Param("receptor") Usuario receptor, @Param("estado") EstadoSolicitud estado);

    @Query("SELECT s FROM SolicitudAmistad s JOIN FETCH s.receptor WHERE s.emisor = :emisor")
    List<SolicitudAmistad> findByEmisorWithReceptor(@Param("emisor") Usuario emisor);

    Optional<SolicitudAmistad> findByEmisorAndReceptorAndEstado(
            Usuario emisor, Usuario receptor, EstadoSolicitud estado);

    boolean existsByEmisorAndReceptorAndEstado(
            Usuario emisor, Usuario receptor, EstadoSolicitud estado);

    @Query("SELECT count(s) > 0 FROM SolicitudAmistad s WHERE s.estado = :estado AND " +
           "((s.emisor = :u1 AND s.receptor = :u2) OR (s.emisor = :u2 AND s.receptor = :u1))")
    boolean existsPendingRequestBetween(@Param("u1") Usuario u1, @Param("u2") Usuario u2, @Param("estado") EstadoSolicitud estado);

    @Modifying
    @Query("DELETE FROM SolicitudAmistad s WHERE (s.emisor = :u1 AND s.receptor = :u2) OR (s.emisor = :u2 AND s.receptor = :u1)")
    void deleteRequestsBetween(@Param("u1") Usuario u1, @Param("u2") Usuario u2);

    void deleteByEmisorOrReceptor(Usuario emisor, Usuario receptor);
}
