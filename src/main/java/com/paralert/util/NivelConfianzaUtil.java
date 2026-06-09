package com.paralert.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NivelConfianzaUtil {

    @Value("${nivel.confianza.registro}")
    private int nivelRegistro;

    @Value("${nivel.confianza.verificado}")
    private int nivelVerificado;

    public int getNivelRegistro() {
        return nivelRegistro;
    }

    public int getNivelVerificado() {
        return nivelVerificado;
    }
}
