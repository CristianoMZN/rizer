package br.com.rizermarketplaces.core.marketplace.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente HTTP para a Meta Graph API.
 * Spring OpenFeign já está disponível no pom, mas para esta fase usamos
 * `java.net.http.HttpClient` para mantermos a dependência leve.
 */
@Service
public class MetaGraphClient {

    private static final Logger log = LoggerFactory.getLogger(MetaGraphClient.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v20.0";
    private static final String OAUTH_BASE = "https://graph.facebook.com/v20.0/oauth/access_token";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Value("${app.meta.app-id:}")
    private String appId;

    @Value("${app.meta.app-secret:}")
    private String appSecret;

    @Value("${app.meta.redirect-uri:http://localhost:3000/app/integracoes/instagram/callback}")
    private String instagramRedirectUri;

    public String buildAuthorizeUrl(String state) {
        StringBuilder sb = new StringBuilder("https://www.facebook.com/v20.0/dialog/oauth");
        sb.append("?client_id=").append(url(appId));
        sb.append("&redirect_uri=").append(url(instagramRedirectUri));
        sb.append("&state=").append(url(state));
        sb.append("&scope=instagram_basic,instagram_content_publish,pages_show_list,pages_read_engagement,business_management");
        return sb.toString();
    }

    /**
     * Troca code → access_token + (opcionalmente) long-lived token.
     */
    public Map<String, Object> exchangeCodeForToken(String code) throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("client_id", appId);
        form.put("client_secret", appSecret);
        form.put("redirect_uri", instagramRedirectUri);
        form.put("code", code);
        JsonNode root = postForm(OAUTH_BASE, form);
        Map<String, Object> out = mapper.convertValue(root, Map.class);
        // Trocar short-lived (1h) por long-lived (60d) quando possível
        if (out.get("access_token") != null) {
            try {
                String longLived = exchangeForLongLivedToken((String) out.get("access_token"));
                out.put("access_token", longLived);
                out.put("expires_in", 60L * 24 * 3600);
            } catch (Exception e) {
                log.warn("[meta-graph] long-lived exchange falhou, mantendo short-lived: {}", e.getMessage());
            }
        }
        return out;
    }

    public String exchangeForLongLivedToken(String shortLived) throws Exception {
        String url = OAUTH_BASE
            + "?grant_type=fb_exchange_token"
            + "&client_id=" + url(appId)
            + "&client_secret=" + url(appId.equals("") ? "" : appSecret)
            + "&fb_exchange_token=" + url(shortLived);
        JsonNode root = httpGetJson(url);
        return root.get("access_token").asText();
    }

    /** GET /me/accounts?fields=id,name,access_token,instagram_business_account */
    public JsonNode meAccounts(String accessToken) throws Exception {
        return httpGetJson(GRAPH_BASE + "/me/accounts?fields=id,name,access_token,instagram_business_account&access_token=" + url(accessToken));
    }

    /** POST /{ig-user-id}/media?image_url=…&caption=…&access_token=… */
    public String createIgMediaContainer(String igUserId, String accessToken, String imageUrl, String caption) throws Exception {
        String url = GRAPH_BASE + "/" + igUserId + "/media"
            + "?image_url=" + url(imageUrl)
            + "&caption=" + url(caption)
            + "&access_token=" + url(accessToken);
        JsonNode root = postEmpty(url);
        return root.get("id").asText();
    }

    /** POST /{ig-user-id}/media_publish */
    public String publishIgMedia(String igUserId, String accessToken, String creationId) throws Exception {
        String url = GRAPH_BASE + "/" + igUserId + "/media_publish"
            + "?creation_id=" + url(creationId)
            + "&access_token=" + url(accessToken);
        JsonNode root = postEmpty(url);
        return root.get("id").asText();
    }

    /** POST /{catalog-id}/products (Meta Catalog) */
    public String createCatalogProduct(String catalogId, String accessToken, Map<String, Object> payload) throws Exception {
        String body = mapper.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder(URI.create(GRAPH_BASE + "/" + catalogId + "/products"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Meta Catalog POST falhou: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode root = mapper.readTree(resp.body());
        return root.path("id").asText();
    }

    public JsonNode httpGetJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET falhou (" + resp.statusCode() + "): " + resp.body());
        }
        return mapper.readTree(resp.body());
    }

    public JsonNode postForm(String url, Map<String, String> form) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (var entry : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(url(entry.getKey())).append('=').append(url(entry.getValue()));
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("POST form falhou (" + resp.statusCode() + "): " + resp.body());
        }
        return mapper.readTree(resp.body());
    }

    public JsonNode postEmpty(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("POST empty falhou (" + resp.statusCode() + "): " + resp.body());
        }
        return mapper.readTree(resp.body());
    }

    private static String url(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
