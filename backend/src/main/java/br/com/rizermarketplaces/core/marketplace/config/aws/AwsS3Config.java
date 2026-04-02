package br.com.rizermarketplaces.core.marketplace.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

// @Configuration: classe que define beans para integração com AWS S3.
@Configuration
public class AwsS3Config {

    // @Bean: registra um bean no contexto do Spring para S3Client.
    // @Value: injeta o valor da propriedade configurada em application.yaml (.properties), por exemplo app.s3.region=us-east-1
    @Bean
    public S3Client s3Client(
        @Value("${app.s3.region}") String region,
        @Value("${app.s3.endpoint:}") String endpoint,
        @Value("${app.s3.path-style-access:false}") boolean pathStyle
    ) {
        var builder = S3Client.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        }
        return builder.build();
    }

    // Bean responsável por gerar URLs assinadas (S3Presigner).
    @Bean
    public S3Presigner s3Presigner(
        @Value("${app.s3.region}") String region,
        @Value("${app.s3.endpoint:}") String endpoint,
        @Value("${app.s3.path-style-access:false}") boolean pathStyle
    ) {
        var builder = S3Presigner.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        }
        return builder.build();
    }
}
