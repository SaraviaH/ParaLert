package com.paralert.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String subirFotoPerfil(MultipartFile archivo, Long usuarioId) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "paralert/perfiles",
                        "public_id", "usuario_" + usuarioId,
                        "overwrite", true,
                        "transformation", "c_fill,g_face,w_200,h_200"
                )
        );
        return (String) resultado.get("secure_url");
    }

    public String subirEvidenciaSos(MultipartFile archivo, Long alertaId) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "paralert/evidencias",
                        "public_id", "alerta_" + alertaId + "_" + System.currentTimeMillis(),
                        "resource_type", "auto"
                )
        );
        return (String) resultado.get("secure_url");
    }

    public String subirImagenZona(MultipartFile archivo, Long zonaId) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "paralert/zonas",
                        "public_id", "zona_" + zonaId + "_" + System.currentTimeMillis(),
                        "resource_type", "auto"
                )
        );
        return (String) resultado.get("secure_url");
    }

    public String subirImagenComentario(MultipartFile archivo, Long comentarioId) throws IOException {
        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "paralert/comentarios",
                        "public_id", "comentario_" + comentarioId + "_" + System.currentTimeMillis(),
                        "resource_type", "auto"
                )
        );
        return (String) resultado.get("secure_url");
    }

    public void eliminarImagen(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Error eliminando imagen de Cloudinary: {}", e.getMessage());
        }
    }
}
