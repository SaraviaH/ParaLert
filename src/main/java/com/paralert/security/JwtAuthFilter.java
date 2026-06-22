package com.paralert.security;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtUtil.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Long id = jwtUtil.extractUsuarioId(token);
            java.util.List<String> roles = jwtUtil.extractRoles(token);
            String nombres = jwtUtil.extractNombres(token);
            String apellidos = jwtUtil.extractApellidos(token);
            Boolean alertasHabilitadas = jwtUtil.extractAlertasHabilitadas(token);
            Boolean verificado = jwtUtil.extractVerificado(token);

            java.util.Set<com.paralert.entity.Rol> setRoles = new java.util.HashSet<>();
            if (roles != null) {
                for (String rName : roles) {
                    setRoles.add(com.paralert.entity.Rol.builder().nombre(rName).build());
                }
            }

            com.paralert.entity.Usuario usuario = com.paralert.entity.Usuario.builder()
                    .id(id)
                    .email(email)
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .roles(setRoles)
                    .alertasHabilitadas(alertasHabilitadas != null ? alertasHabilitadas : true)
                    .verificado(verificado != null ? verificado : false)
                    .estado(com.paralert.entity.enums.EstadoUsuario.ACTIVO)
                    .build();

            UserDetails userDetails = new CustomUserDetails(usuario);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
