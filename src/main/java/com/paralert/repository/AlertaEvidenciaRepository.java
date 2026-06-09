package com.paralert.repository;

import com.paralert.entity.AlertaEvidencia;
import com.paralert.entity.SosAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaEvidenciaRepository extends JpaRepository<AlertaEvidencia, Long> {

    List<AlertaEvidencia> findByAlerta(SosAlerta alerta);
}
