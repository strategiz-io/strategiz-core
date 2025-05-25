package strategiz.service.exchange.coinbase.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Utility class for generating JWT tokens for Coinbase API authentication
 */
@Slf4j
public class CoinbaseJwtUtil {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate a JWT token for Coinbase API authentication
     * 
     * @param apiKey Coinbase API key
     * @param privateKeyPem Coinbase private key in PEM format
     * @return JWT token
     * @throws Exception if there's an error generating the token
     */
    public static String generateJwt(String apiKey, String privateKeyPem) throws Exception {
        log.debug("Generating JWT token for Coinbase API");
        
        // Remove PEM headers/footers and decode
        String privateKeyPEM = privateKeyPem.replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        Instant now = Instant.now();
        long iat = now.getEpochSecond();
        long exp = iat + 120;

        log.debug("JWT token parameters - Issuer: {}, Issued At: {}, Expiration: {}", 
                apiKey, iat, exp);
                
        return Jwts.builder()
                .setIssuer(apiKey)
                .setSubject(apiKey)
                .setAudience("retail_rest_api")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(120)))
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }
}
