package com.paralert.controller;

import com.paralert.dto.request.ChangePasswordRequest;
import com.paralert.dto.request.UpdateProfileRequest;
import com.paralert.dto.request.VerifyDniRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.security.CustomUserDetails;
import com.paralert.service.PerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    // =====================================================
    // VER MI PERFIL
    // GET /api/perfil
    // =====================================================
    @GetMapping
    public ResponseEntity<UserProfileResponse> obtenerPerfil(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(userDetails.getUsuario()));
    }

    // =====================================================
    // ACTUALIZAR PERFIL (nombres/apellidos/teléfono)
    // PUT /api/perfil
    // =====================================================
    @PutMapping
    public ResponseEntity<UserProfileResponse> actualizarPerfil(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(userDetails.getUsuario(), request));
    }

    // =====================================================
    // VERIFICAR DNI
    // POST /api/perfil/verificar-dni
    // =====================================================
    @PostMapping("/verificar-dni")
    public ResponseEntity<UserProfileResponse> verificarDni(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyDniRequest request) {
        return ResponseEntity.ok(perfilService.verificarDni(userDetails.getUsuario(), request));
    }

    // =====================================================
    // CAMBIAR CONTRASEÑA
    // PUT /api/perfil/cambiar-password
    // =====================================================
    @PutMapping("/cambiar-password")
    public ResponseEntity<MessageResponse> cambiarPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(perfilService.cambiarPassword(userDetails.getUsuario(), request));
    }

    // =====================================================
    // SUBIR FOTO DE PERFIL
    // POST /api/perfil/foto
    // =====================================================
    @PostMapping("/foto")
    public ResponseEntity<UserProfileResponse> subirFoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return ResponseEntity.ok(perfilService.subirFotoPerfil(userDetails.getUsuario(), archivo));
    }

    // =====================================================
    // ACTUALIZAR PREFERENCIA DE ALERTAS
    // PUT /api/perfil/alertas
    // =====================================================
    @PutMapping("/alertas")
    public ResponseEntity<UserProfileResponse> actualizarAlertas(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("habilitadas") Boolean habilitadas) {
        return ResponseEntity.ok(perfilService.actualizarAlertas(userDetails.getUsuario(), habilitadas));
    }
}
