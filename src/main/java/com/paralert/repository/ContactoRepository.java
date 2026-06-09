package com.paralert.repository;

import com.paralert.entity.Contacto;
import com.paralert.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    List<Contacto> findByUsuario(Usuario usuario);

    Optional<Contacto> findByUsuarioAndContacto(Usuario usuario, Usuario contacto);

    boolean existsByUsuarioAndContacto(Usuario usuario, Usuario contacto);

    void deleteByUsuarioAndContacto(Usuario usuario, Usuario contacto);
}
