package com.paralert.repository;

import com.paralert.entity.Amigo;
import com.paralert.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmigoRepository extends JpaRepository<Amigo, Long> {

    List<Amigo> findByUsuario(Usuario usuario);

    @Query("SELECT a FROM Amigo a JOIN FETCH a.amigo WHERE a.usuario = :usuario")
    List<Amigo> findByUsuarioWithAmigoEager(@Param("usuario") Usuario usuario);

    Optional<Amigo> findByUsuarioAndAmigo(Usuario usuario, Usuario amigo);

    boolean existsByUsuarioAndAmigo(Usuario usuario, Usuario amigo);

    void deleteByUsuarioAndAmigo(Usuario usuario, Usuario amigo);

    long countByUsuario(Usuario usuario);
}
