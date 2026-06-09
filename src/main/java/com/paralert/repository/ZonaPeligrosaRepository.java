package com.paralert.repository;

import com.paralert.entity.ZonaPeligrosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZonaPeligrosaRepository extends JpaRepository<ZonaPeligrosa, Long> {

    List<ZonaPeligrosa> findAllByOrderByFechaCreacionDesc();
}
