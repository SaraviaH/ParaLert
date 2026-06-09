package com.paralert.repository;

import com.paralert.entity.SolicitudContacto;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findByReceptorAndEstado(Usuario receptor, EstadoSolicitud estado);

    List<SolicitudContacto> findBySolicitanteAndEstado(Usuario solicitante, EstadoSolicitud estado);

    Optional<SolicitudContacto> findBySolicitanteAndReceptorAndEstado(
            Usuario solicitante, Usuario receptor, EstadoSolicitud estado);

    boolean existsBySolicitanteAndReceptorAndEstado(
            Usuario solicitante, Usuario receptor, EstadoSolicitud estado);
}
