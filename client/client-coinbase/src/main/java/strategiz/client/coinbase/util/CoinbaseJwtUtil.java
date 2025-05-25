package strategiz.client.coinbase.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for generating JWT tokens for Coinbase API authentication
 */
public class CoinbaseJwtUtil {

    /**
     * Generate a JWT token for Coinbase API authentication
     *
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return JWT token
     */
    public static String generateJwt(String apiKey, String privateKey) {
        // Create signing key from private key
        Key signingKey = Keys.hmacShaKeyFor(privateKey.getBytes(StandardCharsets.UTF_8));
        
        // Current time in milliseconds
        long now = System.currentTimeMillis();
        
        // Build JWT token
        return Jwts.builder()
                .setSubject(apiKey)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 30000)) // 30 seconds expiration
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
