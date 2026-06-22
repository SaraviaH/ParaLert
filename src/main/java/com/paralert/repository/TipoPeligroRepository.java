package com.paralert.repository;

import com.paralert.entity.TipoPeligro;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

public interface TipoPeligroRepository extends JpaRepository<TipoPeligro, Long> {

    Optional<TipoPeligro> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    @Override
    @Cacheable("tiposPeligro")
    List<TipoPeligro> findAll();

    @Override
    @Cacheable(value = "tiposPeligro", key = "#id")
    Optional<TipoPeligro> findById(Long id);

    @Override
    @CacheEvict(value = "tiposPeligro", allEntries = true)
    <S extends TipoPeligro> S save(S entity);

    @Override
    @CacheEvict(value = "tiposPeligro", allEntries = true)
    void delete(TipoPeligro entity);
}
