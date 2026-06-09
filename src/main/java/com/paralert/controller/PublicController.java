package com.paralert.controller;

import com.paralert.entity.TipoPeligro;
import com.paralert.repository.TipoPeligroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final TipoPeligroRepository tipoPeligroRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Paralert",
                "version", "1.0.0"
        ));
    }

    @GetMapping("/tipos-peligro")
    public ResponseEntity<List<TipoPeligro>> obtenerTiposPeligro() {
        return ResponseEntity.ok(tipoPeligroRepository.findAll());
    }
}
