package com.paralert.repository;

import com.paralert.entity.SosAlerta;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SosAlertaRepository extends JpaRepository<SosAlerta, Long> {

    List<SosAlerta> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    java.util.Optional<SosAlerta> findFirstByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    List<SosAlerta> findByUsuarioAndEstadoOrderByFechaCreacionDesc(Usuario usuario, EstadoAlerta estado);
}
