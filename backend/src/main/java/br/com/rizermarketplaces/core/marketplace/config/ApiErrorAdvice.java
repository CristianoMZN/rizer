package br.com.rizermarketplaces.core.marketplace.config;

import br.com.rizermarketplaces.core.marketplace.audit.AuditSeverity;
import br.com.rizermarketplaces.core.marketplace.audit.AuditService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionStateMachine;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorAdvice.class);
    private final AuditService auditService;

    public ApiErrorAdvice(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(SubscriptionStateMachine.BillingException.class)
    public ResponseEntity<ProblemDetail> handleBilling(SubscriptionStateMachine.BillingException ex) {
        return problem(HttpStatus.PAYMENT_REQUIRED, "billing_error", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCreds(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "bad_credentials", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
            fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("code", "validation_error");
        pd.setProperty("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex) {
        String code = ex.getStatusCode().value() >= 500 ? "internal_error" : "request_error";
        if (ex.getStatusCode().value() == 401) code = "unauthorized";
        else if (ex.getStatusCode().value() == 403) code = "forbidden";
        else if (ex.getStatusCode().value() == 404) code = "not_found";
        else if (ex.getStatusCode().value() == 409) code = "conflict";
        else if (ex.getStatusCode().value() == 402) code = "payment_required";
        return problem(ex.getStatusCode(), code, ex.getReason() != null ? ex.getReason() : ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIAE(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAll(Exception ex, HttpServletRequest request) {
        log.error("[api-error] uncaught em {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        auditService.record("api.error", "endpoint", request.getRequestURI(),
            AuditSeverity.ERROR, Map.of("message", String.valueOf(ex.getMessage())));
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Erro interno do servidor");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : status.getReasonPhrase());
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }

    private ResponseEntity<ProblemDetail> problem(org.springframework.http.HttpStatusCode status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : status.toString());
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
