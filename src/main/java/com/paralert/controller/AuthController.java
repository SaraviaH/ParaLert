package com.paralert.controller;

import com.paralert.dto.request.*;
import com.paralert.dto.response.AuthResponse;
import com.paralert.dto.response.MessageResponse;
import com.paralert.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================
    // REGISTRO - PASO 1: Iniciar (envía código al correo)
    // POST /api/auth/registro/iniciar
    // =====================================================
    @PostMapping("/registro/iniciar")
    public ResponseEntity<MessageResponse> iniciarRegistro(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.iniciarRegistro(request));
    }

    // =====================================================
    // REGISTRO - PASO 2: Verificar código
    // POST /api/auth/registro/verificar
    // =====================================================
    @PostMapping("/registro/verificar")
    public ResponseEntity<MessageResponse> verificarCodigo(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    // =====================================================
    // REGISTRO - PASO 3: Completar (teléfono + contraseña)
    // POST /api/auth/registro/completar
    // =====================================================
    @PostMapping("/registro/completar")
    public ResponseEntity<AuthResponse> completarRegistro(
            @Valid @RequestBody CompleteRegisterRequest request) {
        return ResponseEntity.ok(authService.completarRegistro(request));
    }

    // =====================================================
    // LOGIN
    // POST /api/auth/login
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // =====================================================
    // RECUPERAR PASSWORD - Enviar código
    // POST /api/auth/forgot-password
    // =====================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.enviarCodigoRecuperacion(request));
    }

    // =====================================================
    // RECUPERAR PASSWORD - Resetear
    // POST /api/auth/reset-password
    // =====================================================
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
