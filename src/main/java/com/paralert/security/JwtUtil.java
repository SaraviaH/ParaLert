package com.paralert.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String generateToken(com.paralert.entity.Usuario usuario) {
        java.util.List<String> roles = usuario.getRoles().stream()
                .map(com.paralert.entity.Rol::getNombre)
                .collect(java.util.stream.Collectors.toList());
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("roles", roles)
                .claim("nombres", usuario.getNombres())
                .claim("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "")
                .claim("alertasHabilitadas", usuario.getAlertasHabilitadas())
                .claim("verificado", usuario.getVerificado())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long extractUsuarioId(String token) {
        Number id = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", Number.class);
        return id != null ? id.longValue() : null;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", java.util.List.class);
    }

    public String extractNombres(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("nombres", String.class);
    }

    public String extractApellidos(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("apellidos", String.class);
    }

    public Boolean extractAlertasHabilitadas(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("alertasHabilitadas", Boolean.class);
    }

    public Boolean extractVerificado(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("verificado", Boolean.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
