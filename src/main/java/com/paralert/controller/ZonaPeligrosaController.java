package com.paralert.controller;

import com.paralert.dto.request.ComentarioZonaRequest;
import com.paralert.dto.request.ZonaPeligrosaRequest;
import com.paralert.dto.response.ComentarioZonaResponse;
import com.paralert.dto.response.ZonaPeligrosaResponse;
import com.paralert.security.CustomUserDetails;
import com.paralert.service.ZonaPeligrosaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
public class ZonaPeligrosaController {

    private final ZonaPeligrosaService zonaPeligrosaService;

    // =====================================================
    // LISTAR TODAS LAS ZONAS DE RIESGO
    // GET /api/zonas
    // =====================================================
    @GetMapping
    public ResponseEntity<List<ZonaPeligrosaResponse>> obtenerTodasLasZonas() {
        return ResponseEntity.ok(zonaPeligrosaService.obtenerTodasLasZonas());
    }

    // =====================================================
    // REGISTRAR NUEVA ZONA
    // POST /api/zonas
    // =====================================================
    @PostMapping
    public ResponseEntity<ZonaPeligrosaResponse> crearZona(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ZonaPeligrosaRequest request) {
        return ResponseEntity.ok(zonaPeligrosaService.crearZona(userDetails.getUsuario(), request));
    }

    // =====================================================
    // LISTAR COMENTARIOS DE UNA ZONA
    // GET /api/zonas/{id}/comentarios
    // =====================================================
    @GetMapping("/{id}/comentarios")
    public ResponseEntity<List<ComentarioZonaResponse>> obtenerComentariosDeZona(@PathVariable Long id) {
        return ResponseEntity.ok(zonaPeligrosaService.obtenerComentariosDeZona(id));
    }

    // =====================================================
    // AGREGAR COMENTARIO Y ESTRELLAS A UNA ZONA
    // POST /api/zonas/{id}/comentarios
    // =====================================================
    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioZonaResponse> agregarComentario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ComentarioZonaRequest request) {
        return ResponseEntity.ok(zonaPeligrosaService.agregarComentario(userDetails.getUsuario(), id, request));
    }

    // =====================================================
    // CONFIRMAR INCIDENTE/ZONA DE RIESGO
    // POST /api/zonas/{id}/confirmar
    // =====================================================
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ZonaPeligrosaResponse> confirmarZona(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(zonaPeligrosaService.confirmarZona(userDetails.getUsuario(), id));
    }

    // =====================================================
    // SUBIR FOTO A UNA ZONA DE RIESGO
    // POST /api/zonas/{id}/foto
    // =====================================================
    @PostMapping("/{id}/foto")
    public ResponseEntity<ZonaPeligrosaResponse> subirFotoZona(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo) throws java.io.IOException {
        return ResponseEntity.ok(zonaPeligrosaService.subirFotoZona(userDetails.getUsuario(), id, archivo));
    }

    // =====================================================
    // SUBIR FOTO A UN COMENTARIO
    // POST /api/zonas/comentarios/{comentarioId}/foto
    // =====================================================
    @PostMapping("/comentarios/{comentarioId}/foto")
    public ResponseEntity<ComentarioZonaResponse> subirFotoComentario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long comentarioId,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo) throws java.io.IOException {
        return ResponseEntity.ok(zonaPeligrosaService.subirFotoComentario(userDetails.getUsuario(), comentarioId, archivo));
    }

    // =====================================================
    // EVALUAR PROXIMIDAD DE USUARIO (GUEST O REGISTRADO)
    // POST /api/zonas/evaluar-proximidad
    // =====================================================
    @PostMapping("/evaluar-proximidad")
    public ResponseEntity<com.paralert.dto.response.EvaluacionProximidadResponse> evaluarProximidad(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody com.paralert.dto.request.EvaluacionProximidadRequest request) {
        com.paralert.entity.Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        return ResponseEntity.ok(zonaPeligrosaService.evaluarProximidad(usuario, request));
    }
}
