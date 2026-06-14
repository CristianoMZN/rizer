package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.dto.CepLookupView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente do CepAerto (https://www.cepaberto.com/) — primário.
 * Fallback: retorna null para lat/lng; o front assume HTML5 geolocation.
 *
 * Resposta típica:
 *   {"altitude":571.0,"cep":"99150000","latitude":"-28.4488199955","longitude":"-52.2",
 *    "logradouro":"...","bairro":"...","complemento":"...",
 *    "cidade":{"ddd":54,"ibge":"4311809","nome":"Marau"},
 *    "estado":{"sigla":"RS"}}
 */
@Service
public class CepLookupService {

    private static final Logger log = LoggerFactory.getLogger(CepLookupService.class);
    private static final String BASE = "https://www.cepaberto.com/api/v3/cep";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;
    private static final int CACHE_MAX = 10_000;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${app.cepaberto.token:}")
    private String token;

    public CepLookupView lookup(String rawCep) {
        if (rawCep == null) return null;
        String cep = rawCep.replaceAll("\\D", "");
        if (cep.length() != 8) return null;

        CacheEntry cached = cache.get(cep);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.at) < CACHE_TTL_MS) {
            return cached.value;
        }
        if (cache.size() > CACHE_MAX) {
            cache.clear();
        }

        if (token == null || token.isBlank()) {
            log.debug("CepAberto token ausente; pulando lookup");
            return null;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "?cep=" + cep))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Token token=" + token)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                log.warn("CepAberto {} retornou {}: {}", cep, resp.statusCode(),
                    resp.body() == null ? "" : resp.body().substring(0, Math.min(200, resp.body().length())));
                return null;
            }
            JsonNode n = mapper.readTree(resp.body());
            CepLookupView v = new CepLookupView(
                n.path("cep").asText(cep),
                n.path("logradouro").asText(null),
                n.has("complemento") ? n.path("complemento").asText(null) : null,
                n.path("bairro").asText(null),
                n.path("cidade").path("nome").asText(null),
                n.path("estado").path("sigla").asText(null),
                n.path("cidade").path("ibge").asText(null),
                n.path("cidade").path("ddd").isMissingNode() || n.path("cidade").path("ddd").isNull()
                    ? null : String.valueOf(n.path("cidade").path("ddd").asInt()),
                parseDoubleOrNull(n.path("latitude").asText(null)),
                parseDoubleOrNull(n.path("longitude").asText(null))
            );
            cache.put(cep, new CacheEntry(v, now));
            return v;
        } catch (Exception e) {
            log.warn("Falha no CepAberto lookup do CEP {}: {}", cep, e.getMessage());
            return null;
        }
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }

    private record CacheEntry(CepLookupView value, long at) {}
}
