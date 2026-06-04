package com.esmt.projet.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret:CIS_2026_Secure_Key_Super_Secret_Esmt_M2_Security}")
    private String jwtSecret;

    /*private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }*/
    private Key getSigningKey() {
        // La même chaîne exacte de 32 caractères
        String forceSecret = "CIS_2026_Key_CIS_2026_Key_CIS_2026_Key_";
        return Keys.hmacShaKeyFor(forceSecret.getBytes());
    }
    public boolean getFirstLoginFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        // On récupère le flag, par défaut à false s'il n'existe pas
        return claims.get("firstLogin", Boolean.class) != null && claims.get("firstLogin", Boolean.class);
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Signature JWT invalide: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Format JWT non supporté: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Claims JWT vides: {}", e.getMessage());
        }
        return false;
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }
}