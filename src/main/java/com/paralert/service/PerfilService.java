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
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return mapearPerfil(dbUser);
    }

    // =====================================================
    // EDITAR PERFIL
    // Solo nombres/apellidos si NO está verificado
    // Solo teléfono si SÍ está verificado
    // =====================================================
    @Transactional
    public UserProfileResponse actualizarPerfil(Usuario usuario, UpdateProfileRequest request) {
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!dbUser.getVerificado()) {
            // Antes de verificar DNI puede cambiar nombres y apellidos
            if (request.getNombres() != null && !request.getNombres().isBlank()) {
                dbUser.setNombres(request.getNombres().trim());
            }
            if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
                dbUser.setApellidos(request.getApellidos().trim());
            }
        }

        // Teléfono siempre editable
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            dbUser.setTelefono(request.getTelefono().trim());
        }

        usuarioRepository.save(dbUser);
        return mapearPerfil(dbUser);
    }

    // =====================================================
    // VERIFICAR DNI
    // =====================================================
    @Transactional
    public UserProfileResponse verificarDni(Usuario usuario, VerifyDniRequest request) {
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (dbUser.getVerificado()) {
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

        dbUser.setDni(dni);
        dbUser.setNombres(datos.nombres());
        dbUser.setApellidos(datos.apellidos());
        dbUser.setVerificado(true);
        dbUser.setFechaVerificacion(LocalDateTime.now());
        dbUser.setNivelConfianza(nivelConfianzaUtil.getNivelVerificado());

        usuarioRepository.save(dbUser);
        return mapearPerfil(dbUser);
    }

    // =====================================================
    // CAMBIAR CONTRASEÑA
    // =====================================================
    @Transactional
    public MessageResponse cambiarPassword(Usuario usuario, ChangePasswordRequest request) {
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPasswordActual(), dbUser.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        dbUser.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(dbUser);

        return new MessageResponse("Contraseña actualizada correctamente");
    }

    // =====================================================
    // SUBIR FOTO DE PERFIL
    // =====================================================
    @Transactional
    public UserProfileResponse subirFotoPerfil(Usuario usuario, MultipartFile archivo) throws IOException {
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten imágenes");
        }

        String url = cloudinaryService.subirFotoPerfil(archivo, dbUser.getId());
        dbUser.setFotoPerfil(url);
        usuarioRepository.save(dbUser);

        return mapearPerfil(dbUser);
    }

    @Transactional
    public UserProfileResponse actualizarAlertas(Usuario usuario, Boolean habilitadas) {
        Usuario dbUser = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        dbUser.setAlertasHabilitadas(habilitadas);
        usuarioRepository.save(dbUser);
        log.info("Usuario {} actualizó preferencia de alertas a: {}", dbUser.getEmail(), habilitadas);
        return mapearPerfil(dbUser);
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
