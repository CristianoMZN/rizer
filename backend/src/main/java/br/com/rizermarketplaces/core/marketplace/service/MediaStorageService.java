package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.dto.media.MediaUploadResponse;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// @Service: registra esta classe como um bean de serviço gerenciado pelo Spring.
// Serviços normalmente contêm lógica de negócio e são injetados em controllers.
@Service
public class MediaStorageService {

    private static final String TYPE_PICTURE = "picture";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final String bucket;
    private final String region;
    private final String endpoint;
    private final String keyPrefix;
    private final long presignedDurationMinutes;

    // Construtor com injeção de dependências e valores de propriedades com @Value
    public MediaStorageService(
        S3Client s3Client,
        S3Presigner s3Presigner,
        @Value("${app.s3.bucket}") String bucket,
        @Value("${app.s3.region}") String region,
        @Value("${app.s3.endpoint:}") String endpoint,
        @Value("${app.s3.key-prefix}") String keyPrefix,
        @Value("${app.s3.presigned-duration-minutes}") long presignedDurationMinutes
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.region = region;
        this.endpoint = endpoint;
        this.keyPrefix = keyPrefix;
        this.presignedDurationMinutes = presignedDurationMinutes;
    }

    // @PostConstruct: método executado logo após a criação do bean pelo Spring.
    // Aqui garantimos que ImageIO carregue plugins necessários para leitura/escrita de imagens.
    @PostConstruct
    public void registerImageReaders() {
        ImageIO.scanForPlugins();
    }

    // Ponto central para upload de imagens: valida entrada, aplica regras do contexto,
    // processa a imagem, armazena no S3 e retorna dados para o cliente.
    public MediaUploadResponse uploadPicture(
        MultipartFile file,
        String type,
        String context,
        Integer width,
        Integer height
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        String normalizedType = normalize(type);
        if (!TYPE_PICTURE.equals(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be picture");
        }

        ContextRule rule = ContextRule.resolve(context);
        int targetWidth = width != null && width > 0 ? width : rule.defaultWidth();
        int targetHeight = height != null && height > 0 ? height : rule.defaultHeight();

        ProcessedImage processed = processImage(file, rule, targetWidth, targetHeight);
        String objectKey = buildObjectKey(rule.contextSlug(), processed.format());

        putOnS3(objectKey, processed);
        PresignedGetObjectRequest presignedRequest = buildPresignedGetObjectRequest(objectKey);

        String accessUrl = presignedRequest.url().toString();
        String authorizationToken = extractAuthorizationToken(accessUrl);

        return new MediaUploadResponse(
            normalizedType,
            rule.contextSlug(),
            objectKey,
            buildObjectUrl(objectKey),
            accessUrl,
            authorizationToken,
            processed.width(),
            processed.height(),
            processed.format(),
            processed.bytes().length
        );
    }

    // Processa a imagem: leitura, normalização de cores, renderização para o formato/qualidade desejada
    private ProcessedImage processImage(MultipartFile file, ContextRule rule, int width, int height) {
        try {
            BufferedImage source = ImageIO.read(file.getInputStream());
            if (source == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid image file");
            }

            BufferedImage normalizedSource = rule.format().equals("jpg") ? ensureRgb(source) : source;
            byte[] bytes = renderImage(normalizedSource, width, height, rule.format(), rule.quality());

            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                BufferedImage rendered = ImageIO.read(in);
                if (rendered == null) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "unable to render image output");
                }
                return new ProcessedImage(bytes, rule.format(), rendered.getWidth(), rendered.getHeight());
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to process image", ex);
        }
    }

    // Renderiza a imagem no formato e qualidade desejados (usa Thumbnailator para redimensionamento)
    private byte[] renderImage(BufferedImage source, int width, int height, String format, double quality) throws IOException {
        if ("jpg".equals(format)) {
            BufferedImage resized = Thumbnails.of(source)
                .forceSize(width, height)
                .asBufferedImage();
            return writeJpegWithQuality(resized, quality);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(source)
            .forceSize(width, height)
            .outputFormat(format)
            .outputQuality(quality)
            .toOutputStream(out);
        return out.toByteArray();
    }

    // Escrita de JPEG com controle de qualidade usando ImageWriter
    private byte[] writeJpegWithQuality(BufferedImage image, double quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JPEG writer not found");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality((float) quality);
            }
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    // Converte imagens com canal alfa para RGB preenchendo com branco (necessário antes de gravar JPEG)
    private BufferedImage ensureRgb(BufferedImage source) {
        BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }

    // Envia o objeto para o S3 usando S3Client
    private void putOnS3(String objectKey, ProcessedImage image) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentTypeFromFormat(image.format()))
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(image.bytes()));
    }

    // Gera uma URL pré-assinada usando o S3Presigner para permitir acesso temporário ao objeto privado
    private PresignedGetObjectRequest buildPresignedGetObjectRequest(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignedDurationMinutes))
            .getObjectRequest(getObjectRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest);
    }

    private String buildObjectUrl(String objectKey) {
        if (endpoint != null && !endpoint.isBlank()) {
            String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            return String.format("%s/%s/%s", base, bucket, objectKey);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);
    }

    private String extractAuthorizationToken(String accessUrl) {
        String query = URI.create(accessUrl).getQuery();
        return query == null ? "" : query;
    }

    private String buildObjectKey(String context, String format) {
        String cleanPrefix = keyPrefix.endsWith("/") ? keyPrefix.substring(0, keyPrefix.length() - 1) : keyPrefix;
        return cleanPrefix + "/" + context + "/" + UUID.randomUUID() + "." + format;
    }

    private String contentTypeFromFormat(String format) {
        return switch (format) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ProcessedImage(byte[] bytes, String format, int width, int height) {
    }

    private record ContextRule(String contextSlug, String format, double quality, int defaultWidth, int defaultHeight) {

        private static final Map<String, ContextRule> RULES = Map.of(
            "announce-gallery", new ContextRule("announce-gallery", "webp", 0.70d, 400, 400),
            "store-logo", new ContextRule("store-logo", "png", 0.90d, 512, 512)
        );

        static ContextRule resolve(String rawContext) {
            String normalized = rawContext == null ? "" : rawContext.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "context is required");
            }

            String aliasNormalized = switch (normalized) {
                case "annoince-galery" -> "announce-gallery";
                default -> normalized;
            };

            ContextRule rule = RULES.get(aliasNormalized);
            if (rule == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "unsupported context. supported values: announce-gallery, store-logo"
                );
            }
            return rule;
        }
    }
}
