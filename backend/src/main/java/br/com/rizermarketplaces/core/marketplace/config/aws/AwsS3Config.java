package br.com.rizermarketplaces.core.marketplace.config.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class AwsS3Config {

    private static final Logger logger = LoggerFactory.getLogger(AwsS3Config.class);

    /**
     * Normaliza o endpoint S3, garantindo que tenha um esquema válido.
     * Se o endpoint não tiver esquema (http/https), adiciona https:// automaticamente.
     *
     * @param endpoint o endpoint a ser normalizado
     * @return o endpoint normalizado com esquema, ou null se o input for vazio
     */
    static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }

        String trimmed = endpoint.trim();

        // Se já tem esquema, retorna como está
        if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return trimmed;
        }

        // Caso contrário, adiciona https://
        String normalized = "https://" + trimmed;
        logger.warn("S3 endpoint '{}' is missing URI scheme. Normalizing to '{}'. " +
                    "Consider updating your configuration to include the scheme explicitly.", 
                    trimmed, normalized);
        return normalized;
    }

    @Bean
    public S3Client s3Client(
        @Value("${app.s3.region}") String region,
        @Value("${app.s3.endpoint:}") String endpoint,
        @Value("${app.s3.path-style-access:false}") boolean pathStyle,
        @Value("${aws.access-key-id:}") String accessKeyId,
        @Value("${aws.secret-access-key:}") String secretAccessKey
    ) {
        var creds = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );

        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(creds);

        if (endpoint != null && !endpoint.isBlank()) {
            String normalizedEndpoint = normalizeEndpoint(endpoint);
            if (normalizedEndpoint == null) {
                throw new IllegalArgumentException("Invalid S3 endpoint: " + endpoint);
            }
            builder = builder
                .endpointOverride(URI.create(normalizedEndpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(
        @Value("${app.s3.region}") String region,
        @Value("${app.s3.endpoint:}") String endpoint,
        @Value("${app.s3.path-style-access:false}") boolean pathStyle,
        @Value("${aws.access-key-id:}") String accessKeyId,
        @Value("${aws.secret-access-key:}") String secretAccessKey
    ) {
        var creds = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );

        var builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(creds);

        if (endpoint != null && !endpoint.isBlank()) {
            String normalizedEndpoint = normalizeEndpoint(endpoint);
            if (normalizedEndpoint == null) {
                throw new IllegalArgumentException("Invalid S3 endpoint: " + endpoint);
            }
            builder = builder
                .endpointOverride(URI.create(normalizedEndpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        }
        return builder.build();
    }
}
