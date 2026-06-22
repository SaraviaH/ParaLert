package com.paralert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DniService {

    @Value("${apiperu.base-url:https://apiperu.dev/api}")
    private String baseUrl;

    @Value("${apiperu.token:}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Consulta datos del DNI en apiperu.dev.
     * Endpoint: GET https://apiperu.dev/api/dni/{dni}
     */
    public DniData consultarDni(String dni) {
        String numeroDni = dni == null ? "" : dni.trim();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (apiToken != null && !apiToken.isBlank()) {
                headers.setBearerAuth(apiToken.trim());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            URI url = UriComponentsBuilder
                    .fromUri(URI.create(baseUrl))
                    .pathSegment("dni", numeroDni)
                    .build()
                    .toUri();

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && body != null) {
                if (!Boolean.TRUE.equals(body.get("success"))) {
                    log.warn("Consulta DNI sin éxito para {}: {}", numeroDni, body.get("message"));
                    return null;
                }

                Object dataObj = body.get("data");
                if (dataObj instanceof Map<?, ?> data) {
                    String nombres = obtenerTexto(data, "nombres");
                    String apellidos = construirApellidos(data);

                    if (nombres != null && apellidos != null) {
                        return new DniData(nombres, apellidos);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error consultando DNI {}: {}", numeroDni, e.getMessage());
        }
        return null;
    }

    private String construirApellidos(Map<?, ?> body) {
        String apellidoPaterno = obtenerTexto(body, "apellido_paterno");
        String apellidoMaterno = obtenerTexto(body, "apellido_materno");

        if (apellidoPaterno == null && apellidoMaterno == null) return null;
        if (apellidoPaterno == null) return apellidoMaterno;
        if (apellidoMaterno == null) return apellidoPaterno;

        return apellidoPaterno + " " + apellidoMaterno;
    }

    private String obtenerTexto(Map<?, ?> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    public record DniData(String nombres, String apellidos) {}
}
