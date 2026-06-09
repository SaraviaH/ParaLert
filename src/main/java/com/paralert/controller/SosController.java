package com.paralert.controller;

import com.paralert.dto.request.SosRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.SosResponse;
import com.paralert.security.CustomUserDetails;
import com.paralert.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    // =====================================================
    // DISPARAR SOS
    // POST /api/sos
    // =====================================================
    @PostMapping
    public ResponseEntity<SosResponse> dispararSos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SosRequest request) {
        return ResponseEntity.ok(sosService.dispararSos(userDetails.getUsuario(), request));
    }

    // =====================================================
    // SUBIR EVIDENCIA (foto) a una alerta activa
    // POST /api/sos/{alertaId}/evidencia
    // =====================================================
    @PostMapping("/{alertaId}/evidencia")
    public ResponseEntity<MessageResponse> subirEvidencia(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long alertaId,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return ResponseEntity.ok(
                sosService.subirEvidencia(userDetails.getUsuario(), alertaId, archivo));
    }

    // =====================================================
    // CERRAR/CANCELAR ALERTA
    // PATCH /api/sos/{alertaId}/estado?valor=ATENDIDA
    // =====================================================
    @PatchMapping("/{alertaId}/estado")
    public ResponseEntity<SosResponse> cambiarEstado(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long alertaId,
            @RequestParam("valor") String estado) {
        return ResponseEntity.ok(
                sosService.cambiarEstadoAlerta(userDetails.getUsuario(), alertaId, estado));
    }

    // =====================================================
    // HISTORIAL DE ALERTAS
    // GET /api/sos/historial
    // =====================================================
    @GetMapping("/historial")
    public ResponseEntity<List<SosResponse>> historial(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(sosService.obtenerHistorial(userDetails.getUsuario()));
    }

    // =====================================================
    // VER ALERTA DETALLE
    // GET /api/sos/{alertaId}
    // =====================================================
    @GetMapping("/{alertaId}")
    public ResponseEntity<SosResponse> obtenerAlerta(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long alertaId) {
        return ResponseEntity.ok(sosService.obtenerAlerta(userDetails.getUsuario(), alertaId));
    }
}
