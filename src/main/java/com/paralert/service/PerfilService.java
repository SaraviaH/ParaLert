package com.paralert.service;

import com.paralert.dto.request.ChangePasswordRequest;
import com.paralert.dto.request.UpdateProfileRequest;
import com.paralert.dto.request.VerifyDniRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.entity.Rol;
import com.paralert.entity.Usuario;
import com.paralert.repository.UsuarioRepository;
import com.paralert.util.NivelConfianzaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final DniService dniService;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;
    private final NivelConfianzaUtil nivelConfianzaUtil;

    // =====================================================
    // VER PERFIL
    // =====================================================
    public UserProfileResponse obtenerPerfil(Usuario usuario) {
        return mapearPerfil(usuario);
    }

    // =====================================================
    // EDITAR PERFIL
    // Solo nombres/apellidos si NO está verificado
    // Solo teléfono si SÍ está verificado
    // =====================================================
    @Transactional
    public UserProfileResponse actualizarPerfil(Usuario usuario, UpdateProfileRequest request) {
        if (!usuario.getVerificado()) {
            // Antes de verificar DNI puede cambiar nombres y apellidos
            if (request.getNombres() != null && !request.getNombres().isBlank()) {
                usuario.setNombres(request.getNombres().trim());
            }
            if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
                usuario.setApellidos(request.getApellidos().trim());
            }
        }

        // Teléfono siempre editable
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            usuario.setTelefono(request.getTelefono().trim());
        }

        usuarioRepository.save(usuario);
        return mapearPerfil(usuario);
    }

    // =====================================================
    // VERIFICAR DNI
    // =====================================================
    @Transactional
    public UserProfileResponse verificarDni(Usuario usuario, VerifyDniRequest request) {
        if (usuario.getVerificado()) {
            throw new IllegalStateException("Tu identidad ya está verificada");
        }

        String dni = request.getDni().trim();

        if (usuarioRepository.existsByDni(dni)) {
            throw new IllegalArgumentException("Ese DNI ya está registrado");
        }

        DniService.DniData datos = dniService.consultarDni(dni);
        if (datos == null) {
            throw new IllegalStateException("No se pudo consultar el DNI. Intenta más tarde");
        }

        usuario.setDni(dni);
        usuario.setNombres(datos.nombres());
        usuario.setApellidos(datos.apellidos());
        usuario.setVerificado(true);
        usuario.setFechaVerificacion(LocalDateTime.now());
        usuario.setNivelConfianza(nivelConfianzaUtil.getNivelVerificado());

        usuarioRepository.save(usuario);
        return mapearPerfil(usuario);
    }

    // =====================================================
    // CAMBIAR CONTRASEÑA
    // =====================================================
    @Transactional
    public MessageResponse cambiarPassword(Usuario usuario, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        return new MessageResponse("Contraseña actualizada correctamente");
    }

    // =====================================================
    // SUBIR FOTO DE PERFIL
    // =====================================================
    @Transactional
    public UserProfileResponse subirFotoPerfil(Usuario usuario, MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten imágenes");
        }

        String url = cloudinaryService.subirFotoPerfil(archivo, usuario.getId());
        usuario.setFotoPerfil(url);
        usuarioRepository.save(usuario);

        return mapearPerfil(usuario);
    }

    @Transactional
    public UserProfileResponse actualizarAlertas(Usuario usuario, Boolean habilitadas) {
        usuario.setAlertasHabilitadas(habilitadas);
        usuarioRepository.save(usuario);
        log.info("Usuario {} actualizó preferencia de alertas a: {}", usuario.getEmail(), habilitadas);
        return mapearPerfil(usuario);
    }

    // =====================================================
    // MAPEO
    // =====================================================
    private UserProfileResponse mapearPerfil(Usuario usuario) {
        return UserProfileResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .dni(usuario.getDni())
                .telefono(usuario.getTelefono())
                .fotoPerfil(usuario.getFotoPerfil())
                .verificado(usuario.getVerificado())
                .nivelConfianza(usuario.getNivelConfianza())
                .estado(usuario.getEstado().name())
                .alertasHabilitadas(usuario.getAlertasHabilitadas())
                .roles(usuario.getRoles().stream().map(Rol::getNombre).collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
