package com.example.bankcards.controller.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.UtilityClass;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@UtilityClass
public class Tools {
    private final String jwtSecret = generateBase64EncodedSecret();
    private final long jwtExpirationMs = 10000L;

    private static String generateBase64EncodedSecret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Создает тестовый jwt токен с ролью
     *
     * @param username - логин
     * @param role     - роль
     * @return токен
     */
    public static String generateTestToken(String username, String role) {

        byte[] keyBytes = jwtSecret.getBytes();
        var key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS384)
                .compact();
    }

    /**
     * Создает тестовый jwt токен
     *
     * @param username - логин
     * @return токен
     */
    public static String generateTestToken(String username) {

        byte[] keyBytes = jwtSecret.getBytes();
        var key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS384)
                .compact();
    }

}
