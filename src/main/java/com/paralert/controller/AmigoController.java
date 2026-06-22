package com.paralert.controller;

import com.paralert.dto.request.SendAmigoRequest;
import com.paralert.dto.response.AmigoResponse;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.SolicitudAmistadResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.security.CustomUserDetails;
import com.paralert.service.AmigoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amigos")
@RequiredArgsConstructor
public class AmigoController {

    private final AmigoService amigoService;

    // =====================================================
    // LISTAR MIS AMIGOS
    // GET /api/amigos
    // =====================================================
    @GetMapping
    public ResponseEntity<List<AmigoResponse>> listarAmigos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(amigoService.listarAmigos(userDetails.getUsuario()));
    }

    // =====================================================
    // CAMBIAR PRIORIDAD DE UN AMIGO
    // PUT /api/amigos/{amigoId}/prioridad?valor=FAVORITO
    // =====================================================
    @PutMapping("/{amigoId}/prioridad")
    public ResponseEntity<AmigoResponse> cambiarPrioridad(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long amigoId,
            @RequestParam("valor") String prioridad) {
        return ResponseEntity.ok(amigoService.cambiarPrioridad(userDetails.getUsuario(), amigoId, prioridad));
    }

    // =====================================================
    // ELIMINAR AMIGO
    // DELETE /api/amigos/{amigoId}
    // =====================================================
    @DeleteMapping("/{amigoId}")
    public ResponseEntity<MessageResponse> eliminarAmigo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long amigoId) {
        return ResponseEntity.ok(amigoService.eliminarAmigo(userDetails.getUsuario(), amigoId));
    }

    // =====================================================
    // BUSCAR USUARIO por email o username
    // GET /api/amigos/buscar?q=username
    // =====================================================
    @GetMapping("/buscar")
    public ResponseEntity<UserProfileResponse> buscarUsuario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("q") String identificador) {
        return ResponseEntity.ok(amigoService.buscarUsuario(identificador, userDetails.getUsuario()));
    }

    // =====================================================
    // ENVIAR SOLICITUD DE AMISTAD
    // POST /api/amigos/solicitar
    // =====================================================
    @PostMapping("/solicitar")
    public ResponseEntity<MessageResponse> enviarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SendAmigoRequest request) {
        return ResponseEntity.ok(amigoService.enviarSolicitud(userDetails.getUsuario(), request.getIdentificador()));
    }

    // =====================================================
    // VER SOLICITUDES RECIBIDAS PENDIENTES
    // GET /api/amigos/solicitudes/recibidas
    // =====================================================
    @GetMapping("/solicitudes/recibidas")
    public ResponseEntity<List<SolicitudAmistadResponse>> obtenerSolicitudesRecibidas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(amigoService.obtenerSolicitudesRecibidas(userDetails.getUsuario()));
    }

    // =====================================================
    // VER SOLICITUDES ENVIADAS
    // GET /api/amigos/solicitudes/enviadas
    // =====================================================
    @GetMapping("/solicitudes/enviadas")
    public ResponseEntity<List<SolicitudAmistadResponse>> obtenerSolicitudesEnviadas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(amigoService.obtenerSolicitudesEnviadas(userDetails.getUsuario()));
    }

    // =====================================================
    // ACEPTAR SOLICITUD DE AMISTAD
    // POST /api/amigos/solicitudes/{solicitudId}/aceptar
    // =====================================================
    @PostMapping("/solicitudes/{solicitudId}/aceptar")
    public ResponseEntity<MessageResponse> aceptarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long solicitudId) {
        return ResponseEntity.ok(amigoService.aceptarSolicitud(userDetails.getUsuario(), solicitudId));
    }

    // =====================================================
    // RECHAZAR SOLICITUD DE AMISTAD
    // POST /api/amigos/solicitudes/{solicitudId}/rechazar
    // =====================================================
    @PostMapping("/solicitudes/{solicitudId}/rechazar")
    public ResponseEntity<MessageResponse> rechazarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long solicitudId) {
        return ResponseEntity.ok(amigoService.rechazarSolicitud(userDetails.getUsuario(), solicitudId));
    }
}
