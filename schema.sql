-- ====================================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS PARA EL BACKEND DE PARALERT
-- PostgreSQL DDL - Basado en la implementación de Entidades JPA
-- ====================================================================

-- 1. Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Tabla de Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150),
    dni VARCHAR(8) UNIQUE,
    telefono VARCHAR(20),
    foto_perfil VARCHAR(255),
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_verificacion TIMESTAMP,
    nivel_confianza INTEGER NOT NULL DEFAULT 20,
    cantidad_advertencias INTEGER NOT NULL DEFAULT 0,
    alertas_habilitadas BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

-- 3. Tabla Intermedia Usuario-Roles (Muchos a Muchos)
CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. Tabla de Códigos de Verificación OTP
CREATE TABLE IF NOT EXISTS codigos_verificacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 5. Tabla de Registros de Cuentas Pendientes (Pre-registro)
CREATE TABLE IF NOT EXISTS registros_pendientes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL
);

-- 6. Tabla de Contactos de Confianza (Red Vecinal)
CREATE TABLE IF NOT EXISTS contactos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    contacto_id BIGINT NOT NULL,
    alias VARCHAR(100),
    fecha_agregado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contactos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_contactos_contacto FOREIGN KEY (contacto_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 7. Tabla de Solicitudes de Contacto Vecinal
CREATE TABLE IF NOT EXISTS solicitudes_contacto (
    id BIGSERIAL PRIMARY KEY,
    solicitante_id BIGINT NOT NULL,
    receptor_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_respuesta TIMESTAMP,
    CONSTRAINT fk_solicitudes_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitudes_receptor FOREIGN KEY (receptor_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 8. Tabla de Tipos de Peligros
CREATE TABLE IF NOT EXISTS tipo_peligros (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- 9. Tabla de Zonas Peligrosas (Puntos calientes en el mapa)
CREATE TABLE IF NOT EXISTS zonas_peligrosas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo_peligro_id BIGINT,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    latitud NUMERIC(10, 7) NOT NULL,
    longitud NUMERIC(10, 7) NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL,
    puntaje INTEGER NOT NULL DEFAULT 10,
    radio INTEGER NOT NULL DEFAULT 10,
    estado VARCHAR(20) NOT NULL DEFAULT 'OBSERVACION',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultima_actividad TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    foto_url VARCHAR(255),
    CONSTRAINT fk_zonas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_zonas_tipo_peligro FOREIGN KEY (tipo_peligro_id) REFERENCES tipo_peligros(id)
);

-- 10. Tabla de Comentarios en Zonas
CREATE TABLE IF NOT EXISTS zonas_comentarios (
    id BIGSERIAL PRIMARY KEY,
    zona_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    contenido TEXT NOT NULL,
    calificacion INTEGER NOT NULL,
    foto_url VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comentarios_zona FOREIGN KEY (zona_id) REFERENCES zonas_peligrosas(id) ON DELETE CASCADE,
    CONSTRAINT fk_comentarios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 11. Tabla de Confirmaciones de Zonas Peligrosas
CREATE TABLE IF NOT EXISTS zona_confirmaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    zona_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_zona_confirmacion UNIQUE (usuario_id, zona_id),
    CONSTRAINT fk_confirmaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_confirmaciones_zona FOREIGN KEY (zona_id) REFERENCES zonas_peligrosas(id) ON DELETE CASCADE
);

-- 12. Tabla de Reportes Individuales sobre Incidentes
CREATE TABLE IF NOT EXISTS reportes_incidentes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    zona_id BIGINT NOT NULL,
    tipo_peligro_id BIGINT NOT NULL,
    descripcion TEXT NOT NULL,
    latitud NUMERIC(10, 7) NOT NULL,
    longitud NUMERIC(10, 7) NOT NULL,
    foto_url VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reportes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_reportes_zona FOREIGN KEY (zona_id) REFERENCES zonas_peligrosas(id) ON DELETE CASCADE,
    CONSTRAINT fk_reportes_tipo_peligro FOREIGN KEY (tipo_peligro_id) REFERENCES tipo_peligros(id)
);

-- 13. Tabla de Registros de Proximidad a Zonas Peligrosas
CREATE TABLE IF NOT EXISTS registros_proximidad (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    zona_id BIGINT NOT NULL,
    fecha_ultima_notificacion TIMESTAMP NOT NULL,
    CONSTRAINT uk_usuario_zona_proximidad UNIQUE (usuario_id, zona_id),
    CONSTRAINT fk_proximidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_proximidad_zona FOREIGN KEY (zona_id) REFERENCES zonas_peligrosas(id) ON DELETE CASCADE
);

-- 14. Tabla de Alertas SOS Activas (Botón de Pánico)
CREATE TABLE IF NOT EXISTS sos_alertas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    latitud NUMERIC(10, 7) NOT NULL,
    longitud NUMERIC(10, 7) NOT NULL,
    mensaje TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    CONSTRAINT fk_sos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 15. Tabla de Alertas Enviadas a Contactos de Confianza (Log de envío)
CREATE TABLE IF NOT EXISTS alertas_enviadas (
    id BIGSERIAL PRIMARY KEY,
    alerta_id BIGINT NOT NULL,
    contacto_id BIGINT NOT NULL,
    email VARCHAR(150) NOT NULL,
    enviado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enviadas_alerta FOREIGN KEY (alerta_id) REFERENCES sos_alertas(id) ON DELETE CASCADE,
    CONSTRAINT fk_enviadas_contacto FOREIGN KEY (contacto_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 16. Tabla de Evidencias Multimedia asociadas a las Alertas SOS
CREATE TABLE IF NOT EXISTS alertas_evidencias (
    id BIGSERIAL PRIMARY KEY,
    alerta_id BIGINT NOT NULL,
    url_imagen TEXT NOT NULL,
    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evidencias_alerta FOREIGN KEY (alerta_id) REFERENCES sos_alertas(id) ON DELETE CASCADE
);
