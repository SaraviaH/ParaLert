package com.paralert.repository;

import com.paralert.entity.CodigoVerificacion;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.TipoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    Optional<CodigoVerificacion> findTopByUsuarioAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            Usuario usuario, TipoCodigo tipo);

    void deleteByUsuarioAndTipo(Usuario usuario, TipoCodigo tipo);
}
