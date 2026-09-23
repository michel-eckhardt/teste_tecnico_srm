package com.srm.creditengine.web.error;

import com.srm.creditengine.domain.common.ErrorCode;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;

/**
 * Catalog of the problem types exposed by the API (RFC 9457). Each entry fixes the HTTP status,
 * the stable {@code code} and the Portuguese title returned to clients.
 */
public enum ProblemType {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Requisição inválida"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisição malformada"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Formato de resposta não suportado"),
    CONFLICT(HttpStatus.CONFLICT, "Conflito"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Modificação concorrente"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado"),
    EXCHANGE_RATE_UNAVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "Taxa de câmbio indisponível"),
    EXCHANGE_RATE_STALE(HttpStatus.UNPROCESSABLE_CONTENT, "Taxa de câmbio desatualizada"),
    PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "Pré-condição obrigatória"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");

    private static final String TYPE_BASE_URI = "https://srm.com.br/problems/";

    private final HttpStatus status;
    private final String title;

    ProblemType(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    /** Problem type of a business error code (exhaustive: a new code does not compile unmapped). */
    public static ProblemType of(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND -> RESOURCE_NOT_FOUND;
            case EXCHANGE_RATE_UNAVAILABLE -> EXCHANGE_RATE_UNAVAILABLE;
            case EXCHANGE_RATE_STALE -> EXCHANGE_RATE_STALE;
        };
    }

    /** Fallback for framework exceptions that only carry an HTTP status. */
    public static ProblemType ofStatus(int status) {
        return switch (status) {
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 428 -> PRECONDITION_REQUIRED;
            default -> status >= 500 ? INTERNAL_ERROR : MALFORMED_REQUEST;
        };
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String code() {
        return name();
    }

    public URI type() {
        return URI.create(TYPE_BASE_URI + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
