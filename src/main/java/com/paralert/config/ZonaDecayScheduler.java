package com.paralert.config;

import com.paralert.entity.ZonaPeligrosa;
import com.paralert.repository.ZonaPeligrosaRepository;
import com.paralert.service.ZonaPeligrosaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZonaDecayScheduler {

    private final ZonaPeligrosaRepository zonaPeligrosaRepository;

    // Ejecuta todos los días a la 01:00:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void aplicarDecaimientoDiario() {
        log.info("Iniciando decaimiento diario de puntaje para zonas inactivas...");

        List<ZonaPeligrosa> zonas = zonaPeligrosaRepository.findAll();
        LocalDateTime ahora = LocalDateTime.now();
        int zonasActualizadas = 0;

        for (ZonaPeligrosa z : zonas) {
            if ("CERRADA".equalsIgnoreCase(z.getEstado())) {
                continue;
            }

            LocalDateTime ultimaActividad = z.getFechaUltimaActividad() != null 
                    ? z.getFechaUltimaActividad() 
                    : z.getFechaCreacion();

            long diasInactiva = Duration.between(ultimaActividad, ahora).toDays();
            
            if (diasInactiva >= 7) {
                int puntajeActual = z.getPuntaje() != null ? z.getPuntaje() : 10;
                double porcentajeDescuento = 0.0;

                if (diasInactiva >= 90) {
                    porcentajeDescuento = 0.70; // -70%
                } else if (diasInactiva >= 30) {
                    porcentajeDescuento = 0.30; // -30%
                } else {
                    porcentajeDescuento = 0.10; // -10%
                }

                int descuento = (int) Math.round(puntajeActual * porcentajeDescuento);
                int nuevoPuntaje = Math.max(0, puntajeActual - descuento);

                z.setPuntaje(nuevoPuntaje);
                // Recalcular radio y riesgo
                z.setRadio(ZonaPeligrosaService.calcularRadio(nuevoPuntaje));
                z.setNivelRiesgo(ZonaPeligrosaService.calcularNivelRiesgo(nuevoPuntaje));

                if (nuevoPuntaje <= 30 && "ACTIVA".equals(z.getEstado())) {
                    z.setEstado("OBSERVACION");
                }

                zonaPeligrosaRepository.save(z);
                zonasActualizadas++;
                log.info("Zona #{} inactiva por {} días. Descuento: -{}% ({} pts). Nuevo puntaje: {}", 
                        z.getId(), diasInactiva, (int)(porcentajeDescuento * 100), descuento, nuevoPuntaje);
            }
        }

        log.info("Decaimiento concluido. Zonas actualizadas: {}", zonasActualizadas);
    }
}
