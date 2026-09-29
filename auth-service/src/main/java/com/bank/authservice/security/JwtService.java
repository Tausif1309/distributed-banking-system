package com.bank.authservice.security;

import com.bank.authservice.entity.AuthCredential;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecretKey jwtSecretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(AuthCredential authCredential) {

        Date issuedAt = new Date();

        Date expiration = new Date(
                issuedAt.getTime() + jwtExpiration
        );

        return Jwts.builder()
                .subject(authCredential.getUsername())
                .claim("userId", authCredential.getUserId())
                .claim("role", authCredential.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(jwtSecretKey)
                .compact();
    }
}