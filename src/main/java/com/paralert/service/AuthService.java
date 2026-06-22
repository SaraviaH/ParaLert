package com.paralert.service;

import com.paralert.dto.request.*;
import com.paralert.dto.response.AuthResponse;
import com.paralert.dto.response.MessageResponse;
import com.paralert.entity.*;
import com.paralert.entity.enums.TipoCodigo;
import com.paralert.repository.*;
import com.paralert.security.JwtUtil;
import com.paralert.util.CodigoGenerator;
import com.paralert.util.NivelConfianzaUtil;
import com.paralert.util.UsernameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final RegistroPendienteRepository registroPendienteRepository;
    private final CodigoVerificacionRepository codigoVerificacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final CodigoGenerator codigoGenerator;
    private final UsernameGenerator usernameGenerator;
    private final NivelConfianzaUtil nivelConfianzaUtil;

    @Value("${registro.pendiente.expiracion-minutos}")
    private int expiracionMinutos;

    // =====================================================
    // PASO 1: Iniciar registro
    // =====================================================
    @Transactional
    public MessageResponse iniciarRegistro(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        // Eliminar registro pendiente anterior si existe
        if (registroPendienteRepository.existsByEmail(email)) {
            registroPendienteRepository.deleteByEmail(email);
        }

        String codigo = codigoGenerator.generarCodigo6Digitos();

        RegistroPendiente pendiente = RegistroPendiente.builder()
                .email(email)
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos().trim())
                .codigo(codigo)
                .verificado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos))
                .build();

        registroPendienteRepository.save(pendiente);

        emailService.enviarCodigoVerificacion(email, request.getNombres(), codigo);

        return new MessageResponse("Código enviado a " + email);
    }

    // =====================================================
    // PASO 2: Verificar código
    // =====================================================
    @Transactional
    public MessageResponse verificarCodigo(VerifyCodeRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        RegistroPendiente pendiente = registroPendienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un registro pendiente para ese correo"));

        if (pendiente.getVerificado()) {
            throw new IllegalStateException("El código ya fue verificado");
        }

        if (LocalDateTime.now().isAfter(pendiente.getFechaExpiracion())) {
            throw new IllegalStateException("El código expiró. Inicia el registro nuevamente");
        }

        if (!pendiente.getCodigo().equals(request.getCodigo())) {
            throw new IllegalArgumentException("Código incorrecto");
        }

        pendiente.setVerificado(true);
        registroPendienteRepository.save(pendiente);

        return new MessageResponse("Código verificado correctamente");
    }

    // =====================================================
    // PASO 3: Completar registro
    // =====================================================
    @Transactional
    public AuthResponse completarRegistro(CompleteRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        RegistroPendiente pendiente = registroPendienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un registro pendiente para ese correo"));

        if (!pendiente.getVerificado()) {
            throw new IllegalStateException("Debes verificar tu código primero");
        }

        if (LocalDateTime.now().isAfter(pendiente.getFechaExpiracion())) {
            throw new IllegalStateException("El registro expiró. Inicia el proceso nuevamente");
        }

        if (!request.getPassword().equals(request.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        Rol rolCiudadano = rolRepository.findByNombre("CIUDADANO")
                .orElseThrow(() -> new RuntimeException("Rol CIUDADANO no encontrado"));

        String username = usernameGenerator.generar(pendiente.getNombres(), pendiente.getApellidos());

        Usuario usuario = Usuario.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombres(pendiente.getNombres())
                .apellidos(pendiente.getApellidos())
                .telefono(request.getTelefono().trim())
                .nivelConfianza(nivelConfianzaUtil.getNivelRegistro())
                .build();

        usuario.getRoles().add(rolCiudadano);
        usuarioRepository.save(usuario);

        registroPendienteRepository.delete(pendiente);

        String token = jwtUtil.generateToken(usuario);

        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .nombres(usuario.getNombres())
                .username(usuario.getUsername())
                .nivelConfianza(usuario.getNivelConfianza())
                .verificado(usuario.getVerificado())
                .alertasHabilitadas(usuario.getAlertasHabilitadas())
                .roles(usuario.getRoles().stream().map(Rol::getNombre).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    // =====================================================
    // LOGIN
    // =====================================================
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        }

        if (!usuario.getEstado().name().equals("ACTIVO")) {
            throw new IllegalStateException("Tu cuenta está " + usuario.getEstado().name().toLowerCase());
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(usuario);

        return AuthResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .nombres(usuario.getNombres())
                .username(usuario.getUsername())
                .nivelConfianza(usuario.getNivelConfianza())
                .verificado(usuario.getVerificado())
                .alertasHabilitadas(usuario.getAlertasHabilitadas())
                .roles(usuario.getRoles().stream().map(Rol::getNombre).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    // =====================================================
    // RECUPERAR PASSWORD - Enviar código
    // =====================================================
    @Transactional
    public MessageResponse enviarCodigoRecuperacion(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe cuenta con ese correo"));

        // Eliminar códigos anteriores del mismo tipo
        codigoVerificacionRepository.deleteByUsuarioAndTipo(usuario, TipoCodigo.RECUPERAR_PASSWORD);

        String codigo = codigoGenerator.generarCodigo8Digitos();

        CodigoVerificacion cv = CodigoVerificacion.builder()
                .usuario(usuario)
                .codigo(codigo)
                .tipo(TipoCodigo.RECUPERAR_PASSWORD)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .build();

        codigoVerificacionRepository.save(cv);
        emailService.enviarRecuperarPassword(email, usuario.getNombres(), codigo);

        return new MessageResponse("Código enviado a " + email);
    }

    // =====================================================
    // RECUPERAR PASSWORD - Cambiar contraseña
    // =====================================================
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe cuenta con ese correo"));

        CodigoVerificacion cv = codigoVerificacionRepository
                .findTopByUsuarioAndTipoAndUsadoFalseOrderByFechaCreacionDesc(usuario, TipoCodigo.RECUPERAR_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró código de recuperación válido"));

        if (LocalDateTime.now().isAfter(cv.getFechaExpiracion())) {
            throw new IllegalStateException("El código expiró. Solicita uno nuevo");
        }

        if (!cv.getCodigo().equals(request.getCodigo())) {
            throw new IllegalArgumentException("Código incorrecto");
        }

        cv.setUsado(true);
        codigoVerificacionRepository.save(cv);

        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        return new MessageResponse("Contraseña actualizada correctamente");
    }
}
