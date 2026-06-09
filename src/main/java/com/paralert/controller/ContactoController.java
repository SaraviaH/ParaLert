package com.paralert.controller;

import com.paralert.dto.request.SendContactRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.security.CustomUserDetails;
import com.paralert.service.ContactoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contactos")
@RequiredArgsConstructor
public class ContactoController {

    private final ContactoService contactoService;

    // =====================================================
    // LISTAR MIS CONTACTOS
    // GET /api/contactos
    // =====================================================
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> listarContactos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contactoService.listarContactos(userDetails.getUsuario()));
    }

    // =====================================================
    // BUSCAR USUARIO por email o username
    // GET /api/contactos/buscar?q=email@correo.com
    // =====================================================
    @GetMapping("/buscar")
    public ResponseEntity<UserProfileResponse> buscarUsuario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("q") String identificador) {
        return ResponseEntity.ok(
                contactoService.buscarUsuario(identificador, userDetails.getUsuario()));
    }

    // =====================================================
    // ENVIAR SOLICITUD DE CONTACTO
    // POST /api/contactos/solicitar
    // =====================================================
    @PostMapping("/solicitar")
    public ResponseEntity<MessageResponse> enviarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SendContactRequest request) {
        return ResponseEntity.ok(
                contactoService.enviarSolicitud(userDetails.getUsuario(), request));
    }

    // =====================================================
    // VER SOLICITUDES RECIBIDAS PENDIENTES
    // GET /api/contactos/solicitudes
    // =====================================================
    @GetMapping("/solicitudes")
    public ResponseEntity<List<UserProfileResponse>> obtenerSolicitudes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                contactoService.obtenerSolicitudesRecibidas(userDetails.getUsuario()));
    }

    // =====================================================
    // ACEPTAR SOLICITUD
    // POST /api/contactos/solicitudes/{solicitanteId}/aceptar
    // =====================================================
    @PostMapping("/solicitudes/{solicitanteId}/aceptar")
    public ResponseEntity<MessageResponse> aceptarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long solicitanteId) {
        return ResponseEntity.ok(
                contactoService.aceptarSolicitud(userDetails.getUsuario(), solicitanteId));
    }

    // =====================================================
    // RECHAZAR SOLICITUD
    // POST /api/contactos/solicitudes/{solicitanteId}/rechazar
    // =====================================================
    @PostMapping("/solicitudes/{solicitanteId}/rechazar")
    public ResponseEntity<MessageResponse> rechazarSolicitud(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long solicitanteId) {
        return ResponseEntity.ok(
                contactoService.rechazarSolicitud(userDetails.getUsuario(), solicitanteId));
    }

    // =====================================================
    // ELIMINAR CONTACTO
    // DELETE /api/contactos/{contactoId}
    // =====================================================
    @DeleteMapping("/{contactoId}")
    public ResponseEntity<MessageResponse> eliminarContacto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long contactoId) {
        return ResponseEntity.ok(
                contactoService.eliminarContacto(userDetails.getUsuario(), contactoId));
    }
}
