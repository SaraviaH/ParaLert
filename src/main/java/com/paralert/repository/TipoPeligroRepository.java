package com.paralert.repository;

import com.paralert.entity.TipoPeligro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoPeligroRepository extends JpaRepository<TipoPeligro, Long> {

    Optional<TipoPeligro> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
