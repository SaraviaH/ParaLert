package com.paralert.repository;

import com.paralert.entity.AlertaEnviada;
import com.paralert.entity.SosAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaEnviadaRepository extends JpaRepository<AlertaEnviada, Long> {

    List<AlertaEnviada> findByAlerta(SosAlerta alerta);
}
