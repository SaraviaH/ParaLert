package com.paralert.service;

import com.paralert.dto.response.AmigoResponse;
import com.paralert.dto.response.MessageResponse;
import com.paralert.dto.response.SolicitudAmistadResponse;
import com.paralert.dto.response.UserProfileResponse;
import com.paralert.entity.Amigo;
import com.paralert.entity.SolicitudAmistad;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoSolicitud;
import com.paralert.entity.enums.PrioridadAmigo;
import com.paralert.repository.AmigoRepository;
import com.paralert.repository.SolicitudAmistadRepository;
import com.paralert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmigoService {

    private final UsuarioRepository usuarioRepository;
    private final AmigoRepository amigoRepository;
    private final SolicitudAmistadRepository solicitudAmistadRepository;
    private final EmailService emailService;

    // =====================================================
    // BUSCAR USUARIO por email o username
    // =====================================================
    @Transactional(readOnly = true)
    public UserProfileResponse buscarUsuario(String identificador, Usuario solicitante) {
        String email = identificador.trim().toLowerCase();

        if (!email.contains("@")) {
            throw new IllegalArgumentException("La búsqueda debe realizarse mediante un correo electrónico válido");
        }

        Usuario encontrado = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (encontrado.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException("No puedes agregarte a ti mismo");
        }

        return UserProfileResponse.builder()
                .id(encontrado.getId())
                .username(encontrado.getUsername())
                .nombres(encontrado.getNombres())
                .apellidos(encontrado.getApellidos())
                .email(encontrado.getEmail())
                .fotoPerfil(encontrado.getFotoPerfil())
                .nivelConfianza(encontrado.getNivelConfianza())
                .verificado(encontrado.getVerificado())
                .build();
    }

    // =====================================================
    // ENVIAR SOLICITUD DE AMISTAD
    // =====================================================
    @Transactional
    public MessageResponse enviarSolicitud(Usuario emisor, String identificador) {
        String email = identificador.trim().toLowerCase();

        if (!email.contains("@")) {
            throw new IllegalArgumentException("El identificador debe ser un correo electrónico válido");
        }

        Usuario receptor = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (receptor.getId().equals(emisor.getId())) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        // Validar límite máximo de 6 amigos para el emisor
        if (amigoRepository.countByUsuario(emisor) >= 6) {
            throw new IllegalStateException("Has alcanzado el límite máximo de 6 amigos");
        }

        // Validar límite máximo de 6 amigos para el receptor
        if (amigoRepository.countByUsuario(receptor) >= 6) {
            throw new IllegalStateException("El usuario de destino ya ha alcanzado el límite de 6 amigos");
        }

        // Validar si ya son amigos
        if (amigoRepository.existsByUsuarioAndAmigo(emisor, receptor)) {
            throw new IllegalStateException("Ya eres amigo de este usuario");
        }

        // Validar si ya existe una solicitud pendiente en cualquier dirección
        if (solicitudAmistadRepository.existsPendingRequestBetween(emisor, receptor, EstadoSolicitud.PENDIENTE)) {
            throw new IllegalStateException("Ya existe una solicitud pendiente entre ustedes");
        }

        SolicitudAmistad solicitud = SolicitudAmistad.builder()
                .emisor(emisor)
                .receptor(receptor)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        solicitudAmistadRepository.save(solicitud);

        // Notificar por correo
        emailService.enviarSolicitudContacto(
                receptor.getEmail(),
                emisor.getNombres(),
                emisor.getUsername()
        );

        return new MessageResponse("Solicitud de amistad enviada a " + receptor.getNombres());
    }

    // =====================================================
    // LISTAR SOLICITUDES RECIBIDAS PENDIENTES
    // =====================================================
    @Transactional(readOnly = true)
    public List<SolicitudAmistadResponse> obtenerSolicitudesRecibidas(Usuario usuario) {
        return solicitudAmistadRepository
                .findByReceptorAndEstadoWithEmisor(usuario, EstadoSolicitud.PENDIENTE)
                .stream()
                .map(this::mapearSolicitud)
                .collect(Collectors.toList());
    }

    // =====================================================
    // LISTAR SOLICITUDES ENVIADAS
    // =====================================================
    @Transactional(readOnly = true)
    public List<SolicitudAmistadResponse> obtenerSolicitudesEnviadas(Usuario usuario) {
        return solicitudAmistadRepository
                .findByEmisorWithReceptor(usuario)
                .stream()
                .map(this::mapearSolicitud)
                .collect(Collectors.toList());
    }

    // =====================================================
    // ACEPTAR SOLICITUD
    // =====================================================
    @Transactional
    public MessageResponse aceptarSolicitud(Usuario receptor, Long solicitudId) {
        SolicitudAmistad solicitud = solicitudAmistadRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(receptor.getId())) {
            throw new IllegalArgumentException("No puedes responder a esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Esta solicitud ya ha sido respondida");
        }

        Usuario emisor = solicitud.getEmisor();

        // Validar límite para ambos
        if (amigoRepository.countByUsuario(receptor) >= 6) {
            throw new IllegalStateException("No puedes agregar más amigos. Límite de 6 alcanzado.");
        }
        if (amigoRepository.countByUsuario(emisor) >= 6) {
            throw new IllegalStateException("El solicitante ha alcanzado el límite de 6 amigos.");
        }

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudAmistadRepository.save(solicitud);

        // Crear amistad bidireccional
        Amigo a1 = Amigo.builder().usuario(receptor).amigo(emisor).prioridad(PrioridadAmigo.MEDIA).build();
        Amigo a2 = Amigo.builder().usuario(emisor).amigo(receptor).prioridad(PrioridadAmigo.MEDIA).build();
        amigoRepository.save(a1);
        amigoRepository.save(a2);

        return new MessageResponse("Amistad aceptada con " + emisor.getNombres());
    }

    // =====================================================
    // RECHAZAR SOLICITUD
    // =====================================================
    @Transactional
    public MessageResponse rechazarSolicitud(Usuario receptor, Long solicitudId) {
        SolicitudAmistad solicitud = solicitudAmistadRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(receptor.getId())) {
            throw new IllegalArgumentException("No puedes responder a esta solicitud");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Esta solicitud ya ha sido respondida");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudAmistadRepository.save(solicitud);

        return new MessageResponse("Solicitud rechazada");
    }

    // =====================================================
    // LISTAR AMIGOS
    // =====================================================
    @Transactional(readOnly = true)
    public List<AmigoResponse> listarAmigos(Usuario usuario) {
        return amigoRepository.findByUsuarioWithAmigoEager(usuario)
                .stream()
                .map(this::mapearAmigo)
                .collect(Collectors.toList());
    }

    // =====================================================
    // ELIMINAR AMIGO
    // =====================================================
    @Transactional
    public MessageResponse eliminarAmigo(Usuario usuario, Long amigoId) {
        Usuario amigo = usuarioRepository.findById(amigoId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario amigo no encontrado"));

        if (!amigoRepository.existsByUsuarioAndAmigo(usuario, amigo)) {
            throw new IllegalArgumentException("Este usuario no está en tu lista de amigos");
        }

        // Eliminar relación bidireccional
        amigoRepository.deleteByUsuarioAndAmigo(usuario, amigo);
        amigoRepository.deleteByUsuarioAndAmigo(amigo, usuario);

        // Limpiar solicitudes históricas asociadas para evitar inconsistencias o registros huérfanos
        solicitudAmistadRepository.deleteRequestsBetween(usuario, amigo);

        return new MessageResponse("Amistad eliminada");
    }

    // =====================================================
    // CAMBIAR PRIORIDAD DE AMISTAD
    // =====================================================
    @Transactional
    public AmigoResponse cambiarPrioridad(Usuario usuario, Long amigoId, String prioridadStr) {
        Usuario amigoEnt = usuarioRepository.findById(amigoId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario amigo no encontrado"));

        Amigo amigoRel = amigoRepository.findByUsuarioAndAmigo(usuario, amigoEnt)
                .orElseThrow(() -> new IllegalArgumentException("Relación de amistad no encontrada"));

        PrioridadAmigo prioridad;
        try {
            prioridad = PrioridadAmigo.valueOf(prioridadStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Prioridad inválida. Elige: FAVORITO, ALTA, MEDIA o BAJA");
        }

        amigoRel.setPrioridad(prioridad);
        amigoRepository.save(amigoRel);

        return mapearAmigo(amigoRel);
    }

    // =====================================================
    // MAPPERS
    // =====================================================
    private AmigoResponse mapearAmigo(Amigo a) {
        return AmigoResponse.builder()
                .id(a.getId())
                .amigoId(a.getAmigo().getId())
                .email(a.getAmigo().getEmail())
                .username(a.getAmigo().getUsername())
                .nombres(a.getAmigo().getNombres())
                .apellidos(a.getAmigo().getApellidos())
                .telefono(a.getAmigo().getTelefono())
                .fotoPerfil(a.getAmigo().getFotoPerfil())
                .verificado(a.getAmigo().getVerificado())
                .nivelConfianza(a.getAmigo().getNivelConfianza())
                .prioridad(a.getPrioridad().name())
                .fechaRegistro(a.getFechaRegistro())
                .build();
    }

    private SolicitudAmistadResponse mapearSolicitud(SolicitudAmistad s) {
        return SolicitudAmistadResponse.builder()
                .id(s.getId())
                .emisorId(s.getEmisor().getId())
                .emisorNombres(s.getEmisor().getNombres() + " " + (s.getEmisor().getApellidos() != null ? s.getEmisor().getApellidos() : ""))
                .emisorUsername(s.getEmisor().getUsername())
                .emisorEmail(s.getEmisor().getEmail())
                .emisorFotoPerfil(s.getEmisor().getFotoPerfil())
                .receptorId(s.getReceptor().getId())
                .receptorNombres(s.getReceptor().getNombres() + " " + (s.getReceptor().getApellidos() != null ? s.getReceptor().getApellidos() : ""))
                .receptorUsername(s.getReceptor().getUsername())
                .receptorEmail(s.getReceptor().getEmail())
                .receptorFotoPerfil(s.getReceptor().getFotoPerfil())
                .estado(s.getEstado().name())
                .fechaEnvio(s.getFechaEnvio())
                .build();
    }
}
