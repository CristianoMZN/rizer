package br.com.rizermarketplaces.core.marketplace.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantExceptions {

    private TenantExceptions() {}

    public static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " não encontrado");
    }

    public static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    public static ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }

    public static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    public static ResponseStatusException paymentRequired(String msg) {
        return new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, msg);
    }
}
