package com.paralert.service;

import com.paralert.dto.request.RegisterRequest;
import com.paralert.dto.request.VerifyCodeRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.entity.RegistroPendiente;
import com.paralert.repository.UsuarioRepository;
import com.paralert.repository.RegistroPendienteRepository;
import com.paralert.repository.RolRepository;
import com.paralert.repository.CodigoVerificacionRepository;
import com.paralert.security.JwtUtil;
import com.paralert.util.CodigoGenerator;
import com.paralert.util.NivelConfianzaUtil;
import com.paralert.util.UsernameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private RegistroPendienteRepository registroPendienteRepository;
    @Mock
    private CodigoVerificacionRepository codigoVerificacionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private EmailService emailService;
    @Mock
    private CodigoGenerator codigoGenerator;
    @Mock
    private UsernameGenerator usernameGenerator;
    @Mock
    private NivelConfianzaUtil nivelConfianzaUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    public void testIniciarRegistro_Exito() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("jose.elias@gmail.com");
        req.setNombres("Jose Manuel");
        req.setApellidos("Elias Saravia");

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(codigoGenerator.generarCodigo6Digitos()).thenReturn("123456");

        MessageResponse res = authService.iniciarRegistro(req);

        assertNotNull(res);
        assertTrue(res.getMensaje().contains("Código enviado"));
        verify(registroPendienteRepository, times(1)).save(any(RegistroPendiente.class));
    }

    @Test
    public void testIniciarRegistro_EmailDuplicado() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("jose.elias@gmail.com");
        req.setNombres("Jose");
        req.setApellidos("Elias");

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            authService.iniciarRegistro(req);
        });
    }

    @Test
    public void testVerificarCodigo_Exito() {
        VerifyCodeRequest req = new VerifyCodeRequest();
        req.setEmail("jose.elias@gmail.com");
        req.setCodigo("123456");

        RegistroPendiente pendiente = RegistroPendiente.builder()
                .email("jose.elias@gmail.com")
                .nombres("Jose")
                .apellidos("Elias")
                .codigo("123456")
                .verificado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .build();

        when(registroPendienteRepository.findByEmail("jose.elias@gmail.com")).thenReturn(Optional.of(pendiente));

        MessageResponse res = authService.verificarCodigo(req);

        assertNotNull(res);
        assertEquals("Código verificado correctamente", res.getMensaje());
        assertTrue(pendiente.getVerificado());
        verify(registroPendienteRepository, times(1)).save(pendiente);
    }
}
