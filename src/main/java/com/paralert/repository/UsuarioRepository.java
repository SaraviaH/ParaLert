package com.paralert.repository;

import com.paralert.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByDni(String dni);
}
