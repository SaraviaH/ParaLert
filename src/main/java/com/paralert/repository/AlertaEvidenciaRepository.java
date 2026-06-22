package com.paralert.repository;

import com.paralert.entity.AlertaEvidencia;
import com.paralert.entity.SosAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaEvidenciaRepository extends JpaRepository<AlertaEvidencia, Long> {

    List<AlertaEvidencia> findByAlerta(SosAlerta alerta);
}
