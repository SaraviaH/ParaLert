package com.paralert.service;

import com.paralert.dto.request.SendContactRequest;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.entity.Contacto;
import com.paralert.entity.SolicitudContacto;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoSolicitud;
import com.paralert.repository.ContactoRepository;
import com.paralert.repository.SolicitudContactoRepository;
import com.paralert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactoService {

    private final UsuarioRepository usuarioRepository;
    private final ContactoRepository contactoRepository;
    private final SolicitudContactoRepository solicitudContactoRepository;
    private final EmailService emailService;

    // =====================================================
    // BUSCAR USUARIO por email o username
    // =====================================================
    @Transactional(readOnly = true)
    public UserProfileResponse buscarUsuario(String identificador, Usuario solicitante) {
        String id = identificador.trim();

        Usuario encontrado;
        if (id.contains("@")) {
            encontrado = usuarioRepository.findByEmail(id.toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        } else {
            encontrado = usuarioRepository.findByUsername(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        }

        if (encontrado.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException("No puedes agregarte a ti mismo");
        }

        return UserProfileResponse.builder()
                .id(encontrado.getId())
                .username(encontrado.getUsername())
                .nombres(encontrado.getNombres())
                .apellidos(encontrado.getApellidos())
                .fotoPerfil(encontrado.getFotoPerfil())
                .nivelConfianza(encontrado.getNivelConfianza())
                .verificado(encontrado.getVerificado())
                .build();
    }

    // =====================================================
    // ENVIAR SOLICITUD DE CONTACTO
    // =====================================================
    @Transactional
    public MessageResponse enviarSolicitud(Usuario solicitante, SendContactRequest request) {
        String id = request.getIdentificador().trim();

        Usuario receptor;
        if (id.contains("@")) {
            receptor = usuarioRepository.findByEmail(id.toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        } else {
            receptor = usuarioRepository.findByUsername(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        }

        if (receptor.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException("No puedes agregarte a ti mismo");
        }

        if (contactoRepository.existsByUsuarioAndContacto(solicitante, receptor)) {
            throw new IllegalStateException("Ya es tu contacto");
        }

        if (solicitudContactoRepository.existsBySolicitanteAndReceptorAndEstado(
                solicitante, receptor, EstadoSolicitud.PENDIENTE)) {
            throw new IllegalStateException("Ya enviaste una solicitud a este usuario");
        }

        SolicitudContacto solicitud = SolicitudContacto.builder()
                .solicitante(solicitante)
                .receptor(receptor)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        solicitudContactoRepository.save(solicitud);

        emailService.enviarSolicitudContacto(
                receptor.getEmail(),
                solicitante.getNombres(),
                solicitante.getUsername()
        );

        return new MessageResponse("Solicitud enviada a " + receptor.getNombres());
    }

    // =====================================================
    // VER SOLICITUDES RECIBIDAS (pendientes)
    // =====================================================
    @Transactional(readOnly = true)
    public List<UserProfileResponse> obtenerSolicitudesRecibidas(Usuario usuario) {
        return solicitudContactoRepository
                .findByReceptorAndEstado(usuario, EstadoSolicitud.PENDIENTE)
                .stream()
                .map(sol -> UserProfileResponse.builder()
                        .id(sol.getSolicitante().getId())
                        .username(sol.getSolicitante().getUsername())
                        .nombres(sol.getSolicitante().getNombres())
                        .apellidos(sol.getSolicitante().getApellidos())
                        .fotoPerfil(sol.getSolicitante().getFotoPerfil())
                        .nivelConfianza(sol.getSolicitante().getNivelConfianza())
                        .verificado(sol.getSolicitante().getVerificado())
                        .build())
                .collect(Collectors.toList());
    }

    // =====================================================
    // ACEPTAR SOLICITUD
    // =====================================================
    @Transactional
    public MessageResponse aceptarSolicitud(Usuario receptor, Long solicitanteId) {
        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        SolicitudContacto solicitud = solicitudContactoRepository
                .findBySolicitanteAndReceptorAndEstado(solicitante, receptor, EstadoSolicitud.PENDIENTE)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró solicitud pendiente"));

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudContactoRepository.save(solicitud);

        // Relación bidireccional
        Contacto c1 = Contacto.builder().usuario(receptor).contacto(solicitante).build();
        Contacto c2 = Contacto.builder().usuario(solicitante).contacto(receptor).build();
        contactoRepository.save(c1);
        contactoRepository.save(c2);

        return new MessageResponse("Contacto agregado: " + solicitante.getNombres());
    }

    // =====================================================
    // RECHAZAR SOLICITUD
    // =====================================================
    @Transactional
    public MessageResponse rechazarSolicitud(Usuario receptor, Long solicitanteId) {
        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        SolicitudContacto solicitud = solicitudContactoRepository
                .findBySolicitanteAndReceptorAndEstado(solicitante, receptor, EstadoSolicitud.PENDIENTE)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró solicitud pendiente"));

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudContactoRepository.save(solicitud);

        return new MessageResponse("Solicitud rechazada");
    }

    // =====================================================
    // LISTAR MIS CONTACTOS
    // =====================================================
    @Transactional(readOnly = true)
    public List<UserProfileResponse> listarContactos(Usuario usuario) {
        return contactoRepository.findByUsuario(usuario)
                .stream()
                .map(c -> UserProfileResponse.builder()
                        .id(c.getContacto().getId())
                        .username(c.getContacto().getUsername())
                        .nombres(c.getContacto().getNombres())
                        .apellidos(c.getContacto().getApellidos())
                        .fotoPerfil(c.getContacto().getFotoPerfil())
                        .nivelConfianza(c.getContacto().getNivelConfianza())
                        .verificado(c.getContacto().getVerificado())
                        .build())
                .collect(Collectors.toList());
    }

    // =====================================================
    // ELIMINAR CONTACTO
    // =====================================================
    @Transactional
    public MessageResponse eliminarContacto(Usuario usuario, Long contactoId) {
        Usuario contacto = usuarioRepository.findById(contactoId)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado"));

        if (!contactoRepository.existsByUsuarioAndContacto(usuario, contacto)) {
            throw new IllegalArgumentException("Ese usuario no es tu contacto");
        }

        contactoRepository.deleteByUsuarioAndContacto(usuario, contacto);
        contactoRepository.deleteByUsuarioAndContacto(contacto, usuario);

        return new MessageResponse("Contacto eliminado");
    }
}
