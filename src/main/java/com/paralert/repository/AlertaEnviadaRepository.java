package com.paralert.repository;

import com.paralert.entity.AlertaEnviada;
import com.paralert.entity.SosAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaEnviadaRepository extends JpaRepository<AlertaEnviada, Long> {

    List<AlertaEnviada> findByAlerta(SosAlerta alerta);
}
