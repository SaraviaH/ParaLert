package com.paralert.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodigoGenerator {

    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generarCodigo6Digitos() {
        return generarCodigo(6);
    }

    public String generarCodigo8Digitos() {
        return generarCodigo(8);
    }

    private String generarCodigo(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
