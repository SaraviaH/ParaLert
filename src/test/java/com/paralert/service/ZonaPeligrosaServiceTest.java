package com.paralert.service;

import com.paralert.dto.request.EvaluacionProximidadRequest;
import com.paralert.dto.response.EvaluacionProximidadResponse;
import com.paralert.entity.Usuario;
import com.paralert.entity.ZonaPeligrosa;
import com.paralert.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ZonaPeligrosaServiceTest {

    @Mock
    private ZonaPeligrosaRepository zonaPeligrosaRepository;
    @Mock
    private ComentarioZonaRepository comentarioZonaRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private TipoPeligroRepository tipoPeligroRepository;
    @Mock
    private ContactoRepository contactoRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private RegistroProximidadRepository registroProximidadRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private ConfirmacionRepository confirmacionRepository;

    @InjectMocks
    private ZonaPeligrosaService zonaPeligrosaService;

    @Test
    public void testCalcularRadio_RangosEstablecidos() {
        assertEquals(10, ZonaPeligrosaService.calcularRadio(10));
        assertEquals(25, ZonaPeligrosaService.calcularRadio(50));
        assertEquals(37, ZonaPeligrosaService.calcularRadio(75)); // Interpolado (75-50)*25/50 + 25 = 37
        assertEquals(130, ZonaPeligrosaService.calcularRadio(250));
    }

    @Test
    public void testCalcularNivelRiesgo() {
        assertEquals("OBSERVACION", ZonaPeligrosaService.calcularNivelRiesgo(20));
        assertEquals("BAJO", ZonaPeligrosaService.calcularNivelRiesgo(50));
        assertEquals("MEDIO", ZonaPeligrosaService.calcularNivelRiesgo(100));
        assertEquals("ALTO", ZonaPeligrosaService.calcularNivelRiesgo(200));
        assertEquals("CRITICO", ZonaPeligrosaService.calcularNivelRiesgo(300));
    }

    @Test
    public void testEvaluarProximidad_FueraDeRadio() {
        Usuario usuario = new Usuario();
        usuario.setAlertasHabilitadas(true);

        ZonaPeligrosa zona = new ZonaPeligrosa();
        zona.setId(1L);
        zona.setTitulo("Zona de Peligro");
        zona.setLatitud(new BigDecimal("-12.046374"));
        zona.setLongitud(new BigDecimal("-77.042793"));
        zona.setRadio(50);
        zona.setNivelRiesgo("ALTO");

        when(zonaPeligrosaRepository.findAll()).thenReturn(Collections.singletonList(zona));

        EvaluacionProximidadRequest req = new EvaluacionProximidadRequest();
        req.setLatitud(new BigDecimal("-12.050000"));
        req.setLongitud(new BigDecimal("-77.045000"));

        EvaluacionProximidadResponse res = zonaPeligrosaService.evaluarProximidad(usuario, req);

        assertNotNull(res);
        assertEquals("NINGUNO", res.getNivelRiesgo());
        assertFalse(res.getNotificadoContactos());
    }
}
