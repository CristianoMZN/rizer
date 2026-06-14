package br.com.rizermarketplaces.core.marketplace.tools;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private RequestUtils() {}

    public static String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return comma >= 0 ? fwd.substring(0, comma).trim() : fwd.trim();
        }
        return request.getRemoteAddr();
    }
}
