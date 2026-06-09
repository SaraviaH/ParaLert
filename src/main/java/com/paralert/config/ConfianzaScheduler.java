package com.paralert.config;

import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoUsuario;
import com.paralert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfianzaScheduler {

    private final UsuarioRepository usuarioRepository;

    // =====================================================
    // CRON SEMANAL: Todos los lunes a las 00:00:00
    // =====================================================
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void recuperarConfianzaSemanal() {
        log.info("Iniciando proceso semanal de recuperación de confianza de usuarios...");

        List<Usuario> usuarios = usuarioRepository.findAll();
        int actualizados = 0;

        for (Usuario u : usuarios) {
            // Solo recuperar confianza para usuarios activos/no bloqueados
            if (u.getEstado() == EstadoUsuario.ACTIVO) {
                if (u.getNivelConfianza() < 100) {
                    int nuevaConfianza = Math.min(100, u.getNivelConfianza() + 5);
                    u.setNivelConfianza(nuevaConfianza);
                    usuarioRepository.save(u);
                    actualizados++;
                }
            }
        }

        log.info("Proceso de recuperación semanal de confianza concluido. Se actualizaron {} usuarios.", actualizados);
    }
}
