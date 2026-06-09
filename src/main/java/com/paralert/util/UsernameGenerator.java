package com.paralert.util;

import com.paralert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class UsernameGenerator {

    private final UsuarioRepository usuarioRepository;

    /**
     * Genera un username único basado en nombres y apellidos.
     * Ejemplo: Jose Saravia → josesaravia, josesaravia1, josesaravia2...
     */
    public String generar(String nombres, String apellidos) {
        String base = limpiar(nombres) + limpiar(apellidos);
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        String candidato = base;
        int contador = 1;

        while (usuarioRepository.existsByUsername(candidato)) {
            candidato = base + contador;
            contador++;
        }

        return candidato;
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        // Toma solo la primera palabra
        String primeraPalabra = texto.trim().split("\\s+")[0];
        // Elimina tildes
        String normalizado = Normalizer.normalize(primeraPalabra, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Solo letras minúsculas
        return normalizado.toLowerCase().replaceAll("[^a-z]", "");
    }
}
