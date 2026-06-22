package com.paralert.repository;

import com.paralert.entity.RegistroPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroPendienteRepository extends JpaRepository<RegistroPendiente, Long> {

    Optional<RegistroPendiente> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
