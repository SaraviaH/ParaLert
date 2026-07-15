package com.paralert.config;

import com.paralert.entity.Rol;
import com.paralert.entity.Usuario;
import com.paralert.entity.enums.EstadoUsuario;
import com.paralert.repository.RolRepository;
import com.paralert.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // =====================================================
    // CREDENCIALES DEL ADMINISTRADOR PREDEFINIDO
    // =====================================================
    public static final String ADMIN_EMAIL = "admin@paralert.com";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "AdminPassword123#";

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Asegurando consistencia de base de datos...");
            
            // Tabla: usuarios
            jdbcTemplate.execute("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS alertas_habilitadas boolean DEFAULT true");
            jdbcTemplate.execute("UPDATE usuarios SET alertas_habilitadas = true WHERE alertas_habilitadas IS NULL");
            jdbcTemplate.execute("ALTER TABLE usuarios ALTER COLUMN alertas_habilitadas SET NOT NULL");

            // Tabla: zonas_peligrosas - estado
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ADD COLUMN IF NOT EXISTS estado varchar(20) DEFAULT 'OBSERVACION'");
            jdbcTemplate.execute("UPDATE zonas_peligrosas SET estado = 'OBSERVACION' WHERE estado IS NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ALTER COLUMN estado SET NOT NULL");

            // Tabla: zonas_peligrosas - puntaje
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ADD COLUMN IF NOT EXISTS puntaje integer DEFAULT 10");
            jdbcTemplate.execute("UPDATE zonas_peligrosas SET puntaje = 10 WHERE puntaje IS NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ALTER COLUMN puntaje SET NOT NULL");

            // Tabla: zonas_peligrosas - radio
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ADD COLUMN IF NOT EXISTS radio integer DEFAULT 10");
            jdbcTemplate.execute("UPDATE zonas_peligrosas SET radio = 10 WHERE radio IS NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ALTER COLUMN radio SET NOT NULL");

            // Tabla: zonas_peligrosas - fecha_ultima_actividad
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ADD COLUMN IF NOT EXISTS fecha_ultima_actividad timestamp DEFAULT CURRENT_TIMESTAMP");
            jdbcTemplate.execute("UPDATE zonas_peligrosas SET fecha_ultima_actividad = fecha_creacion WHERE fecha_ultima_actividad IS NULL");
            jdbcTemplate.execute("UPDATE zonas_peligrosas SET fecha_ultima_actividad = CURRENT_TIMESTAMP WHERE fecha_ultima_actividad IS NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_peligrosas ALTER COLUMN fecha_ultima_actividad SET NOT NULL");

            // Tabla: zonas_comentarios (ComentarioZona) - columnas de moderación
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ADD COLUMN IF NOT EXISTS sospechoso boolean DEFAULT false");
            jdbcTemplate.execute("UPDATE zonas_comentarios SET sospechoso = false WHERE sospechoso IS NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ALTER COLUMN sospechoso SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ADD COLUMN IF NOT EXISTS juicio_tipo varchar(50)");
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ADD COLUMN IF NOT EXISTS juicio_justificacion text");
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ADD COLUMN IF NOT EXISTS evaluador varchar(50)");
            jdbcTemplate.execute("ALTER TABLE zonas_comentarios ADD COLUMN IF NOT EXISTS fecha_evaluacion timestamp");

            // Tabla: reportes_incidentes (Reporte) - columnas de moderación
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ADD COLUMN IF NOT EXISTS sospechoso boolean DEFAULT false");
            jdbcTemplate.execute("UPDATE reportes_incidentes SET sospechoso = false WHERE sospechoso IS NULL");
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ALTER COLUMN sospechoso SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ADD COLUMN IF NOT EXISTS juicio_tipo varchar(50)");
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ADD COLUMN IF NOT EXISTS juicio_justificacion text");
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ADD COLUMN IF NOT EXISTS evaluador varchar(50)");
            jdbcTemplate.execute("ALTER TABLE reportes_incidentes ADD COLUMN IF NOT EXISTS fecha_evaluacion timestamp");

            // Tabla: palabras_sospechosas
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS palabras_sospechosas (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "palabra VARCHAR(100) UNIQUE NOT NULL, " +
                    "categoria VARCHAR(50) NOT NULL, " +
                    "fecha_creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL" +
                    ")");

            log.info("Base de datos saneada con éxito.");
        } catch (Exception e) {
            log.error("Error saneando la base de datos: {}", e.getMessage(), e);
        }

        // 1. Inicializar roles si no existen
        Rol rolAdmin = inicializarRolSiNoExiste("ADMIN");
        inicializarRolSiNoExiste("CIUDADANO");

        // 2. Crear o actualizar administrador para asegurar que tenga únicamente el rol ADMIN
        java.util.Optional<Usuario> adminOpt = usuarioRepository.findByEmail(ADMIN_EMAIL);
        if (adminOpt.isPresent()) {
            Usuario admin = adminOpt.get();
            admin.setRoles(Set.of(rolAdmin)); // Fuerza solo rol ADMIN, sin mezclar con CIUDADANO
            admin.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(admin);
            log.info("Cuenta de administrador existente encontrada. Rol ADMIN forzado y limpiado de otros roles.");
        } else {
            log.info("Iniciando creación de cuenta de administrador predefinida...");

            Usuario admin = Usuario.builder()
                    .email(ADMIN_EMAIL)
                    .username(ADMIN_USERNAME)
                    .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                    .nombres("Administrador")
                    .apellidos("General")
                    .dni("00000000")
                    .telefono("999999999")
                    .verificado(true)
                    .nivelConfianza(100)
                    .estado(EstadoUsuario.ACTIVO)
                    .roles(Set.of(rolAdmin)) // Solo rol ADMIN
                    .build();

            usuarioRepository.save(admin);

            log.info("=====================================================");
            log.info("¡CUENTA DE ADMINISTRADOR CREADA CON ÉXITO!");
            log.info("Correo: {}", ADMIN_EMAIL);
            log.info("Username: {}", ADMIN_USERNAME);
            log.info("Contraseña: {}", ADMIN_PASSWORD);
            log.info("=====================================================");
        }
    }

    private Rol inicializarRolSiNoExiste(String nombreRol) {
        return rolRepository.findByNombre(nombreRol)
                .orElseGet(() -> {
                    Rol nuevoRol = Rol.builder()
                            .nombre(nombreRol)
                            .build();
                    Rol guardado = rolRepository.save(nuevoRol);
                    log.info("Rol '{}' inicializado en la base de datos.", nombreRol);
                    return guardado;
                });
    }
}
