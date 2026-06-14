package br.com.rizermarketplaces.core.marketplace.config.aws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para AwsS3Config.
 * Foca na validação do método normalizeEndpoint() que garante que endpoints S3
 * sempre tenham um esquema válido (https:// ou http://).
 */
class AwsS3ConfigTest {

    @Test
    void shouldNormalizeEndpointWithoutScheme() {
        // Arrange
        String endpointWithoutScheme = "br-se1.magaluobjects.com";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(endpointWithoutScheme);

        // Assert
        assertEquals("https://br-se1.magaluobjects.com", normalized);
    }

    @Test
    void shouldKeepEndpointWithHttpsScheme() {
        // Arrange
        String endpointWithScheme = "https://br-se1.magaluobjects.com";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(endpointWithScheme);

        // Assert
        assertEquals("https://br-se1.magaluobjects.com", normalized);
    }

    @Test
    void shouldKeepEndpointWithHttpScheme() {
        // Arrange
        String endpointWithScheme = "http://localhost:9000";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(endpointWithScheme);

        // Assert
        assertEquals("http://localhost:9000", normalized);
    }

    @Test
    void shouldHandleNullEndpoint() {
        // Act
        String normalized = AwsS3Config.normalizeEndpoint(null);

        // Assert
        assertNull(normalized);
    }

    @Test
    void shouldHandleEmptyEndpoint() {
        // Act
        String normalizedEmpty = AwsS3Config.normalizeEndpoint("");
        String normalizedBlank = AwsS3Config.normalizeEndpoint("   ");

        // Assert
        assertNull(normalizedEmpty);
        assertNull(normalizedBlank);
    }

    @Test
    void shouldTrimWhitespace() {
        // Arrange
        String endpointWithWhitespace = "  br-se1.magaluobjects.com  ";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(endpointWithWhitespace);

        // Assert
        assertEquals("https://br-se1.magaluobjects.com", normalized);
    }

    @Test
    void shouldHandleComplexUrls() {
        // Arrange
        String complexUrl = "s3.us-west-2.amazonaws.com";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(complexUrl);

        // Assert
        assertEquals("https://s3.us-west-2.amazonaws.com", normalized);
    }

    @Test
    void shouldHandleCustomPort() {
        // Arrange
        String endpointWithPort = "minio.example.com:9000";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(endpointWithPort);

        // Assert
        assertEquals("https://minio.example.com:9000", normalized);
    }

    @Test
    void shouldHandleIpAddress() {
        // Arrange
        String ipEndpoint = "192.168.1.100:9000";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(ipEndpoint);

        // Assert
        assertEquals("https://192.168.1.100:9000", normalized);
    }

    @Test
    void shouldPreservePathInUrl() {
        // Arrange
        String urlWithPath = "https://storage.googleapis.com/bucket-name";

        // Act
        String normalized = AwsS3Config.normalizeEndpoint(urlWithPath);

        // Assert
        assertEquals("https://storage.googleapis.com/bucket-name", normalized);
    }
}
