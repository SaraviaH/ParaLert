package com.paralert.repository;

import com.paralert.entity.SosAlerta;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SosAlertaRepository extends JpaRepository<SosAlerta, Long> {

    List<SosAlerta> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    List<SosAlerta> findByUsuarioAndEstadoOrderByFechaCreacionDesc(Usuario usuario, EstadoAlerta estado);
}
