package com.paralert.repository;

import com.paralert.entity.PalabraSospechosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PalabraSospechosaRepository extends JpaRepository<PalabraSospechosa, Long> {
    Optional<PalabraSospechosa> findByPalabra(String palabra);
    boolean existsByPalabra(String palabra);
}
