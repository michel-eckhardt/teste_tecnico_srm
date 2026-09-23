package com.srm.creditengine.web.error;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.web.support.CorrelationIdFilter;
import com.srm.creditengine.web.support.InvalidHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Translates every exception into an RFC 9457 {@code application/problem+json} response with the
 * extra members {@code code}, {@code correlationId} and (for input errors) {@code errors[]}.
 *
 * <p>Stack traces and internal messages are never returned to the client: unexpected failures are
 * logged with the correlation id and answered with a generic {@code INTERNAL_ERROR}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_DETAIL =
            "Ocorreu um erro inesperado. Informe o correlationId ao suporte.";

    // ------------------------------------------------------------------------------------------
    // Business and persistence exceptions
    // ------------------------------------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        ProblemType type = ProblemType.of(ex.code());
        log.info("Business rule rejected the request: code={}", ex.code());
        return problem(type, ex.getMessage(), List.of(), new HttpHeaders(), request);
    }

    @ExceptionHandler(InvalidHeaderException.class)
    ResponseEntity<Object> handleInvalidHeader(InvalidHeaderException ex, WebRequest request) {
        return problem(
                ProblemType.VALIDATION_ERROR,
                "Header inválido.",
                List.of(new FieldViolation(ex.header(), ex.getMessage())),
                new HttpHeaders(),
                request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Concurrent modification detected: {}", ex.getClass().getSimpleName());
        return problem(
                ProblemType.CONCURRENT_MODIFICATION,
                "O recurso foi alterado por outra requisição. Recarregue e tente novamente.",
                List.of(),
                new HttpHeaders(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation", ex);
        return problem(
                ProblemType.CONFLICT,
                "A operação viola uma restrição de integridade dos dados.",
                List.of(),
                new HttpHeaders(),
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error while processing {}", path(request), ex);
        return problem(ProblemType.INTERNAL_ERROR, INTERNAL_ERROR_DETAIL, List.of(), new HttpHeaders(), request);
    }

    // ------------------------------------------------------------------------------------------
    // Spring MVC exceptions with a richer (field level) answer
    // ------------------------------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.add(new FieldViolation(error.getField(), messageOf(error)));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            violations.add(new FieldViolation(error.getObjectName(), error.getDefaultMessage()));
        }
        return problem(ProblemType.VALIDATION_ERROR, "Um ou mais campos são inválidos.", violations, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                if (error instanceof FieldError fieldError) {
                    violations.add(new FieldViolation(fieldError.getField(), messageOf(fieldError)));
                } else {
                    violations.add(new FieldViolation(name, error.getDefaultMessage()));
                }
            }
        });
        return problem(
                ProblemType.VALIDATION_ERROR, "Um ou mais parâmetros são inválidos.", violations, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getMostSpecificCause() instanceof MismatchedInputException mismatch
                && !mismatch.getPath().isEmpty()) {
            FieldViolation violation = new FieldViolation(jsonPath(mismatch), describe(mismatch));
            return problem(
                    ProblemType.VALIDATION_ERROR,
                    "Um ou mais campos possuem formato inválido.",
                    List.of(violation),
                    headers,
                    request);
        }
        return problem(
                ProblemType.MALFORMED_REQUEST,
                "O corpo da requisição está ausente ou não é um JSON válido.",
                List.of(),
                headers,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : String.valueOf(ex.getPropertyName());
        FieldViolation violation =
                new FieldViolation(name, "valor '%s' possui formato inválido".formatted(ex.getValue()));
        return problem(
                ProblemType.VALIDATION_ERROR,
                "Um ou mais parâmetros possuem formato inválido.",
                List.of(violation),
                headers,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        FieldViolation violation = new FieldViolation(ex.getParameterName(), "parâmetro obrigatório");
        return problem(
                ProblemType.VALIDATION_ERROR, "Parâmetro obrigatório ausente.", List.of(violation), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missing) {
            String header = missing.getHeaderName();
            if (HttpHeaders.IF_MATCH.equalsIgnoreCase(header)) {
                // RFC 6585: the origin server requires the request to be conditional.
                return problem(
                        ProblemType.PRECONDITION_REQUIRED,
                        "Informe o header If-Match com a versão (ETag) atual do recurso.",
                        List.of(),
                        headers,
                        request);
            }
            return problem(
                    ProblemType.VALIDATION_ERROR,
                    "Header obrigatório ausente.",
                    List.of(new FieldViolation(header, "header obrigatório")),
                    headers,
                    request);
        }
        return problem(ProblemType.VALIDATION_ERROR, "Requisição inválida.", List.of(), headers, request);
    }

    /** Every other framework exception: keep its status, normalize the body. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemType type = ProblemType.ofStatus(statusCode.value());
        if (type == ProblemType.INTERNAL_ERROR) {
            log.error("Framework error while processing {}", path(request), ex);
            return problem(type, INTERNAL_ERROR_DETAIL, List.of(), headers, request);
        }
        log.debug("Request rejected by the framework: {}", ex.getMessage());
        return problem(type, type.title() + ".", List.of(), headers, request);
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private ResponseEntity<Object> problem(
            ProblemType type, String detail, List<FieldViolation> violations, HttpHeaders headers, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(type.status(), detail);
        body.setType(type.type());
        body.setTitle(type.title());
        body.setInstance(URI.create(path(request)));
        body.setProperty("code", type.code());
        body.setProperty("correlationId", CorrelationIdFilter.current());
        if (!violations.isEmpty()) {
            body.setProperty("errors", violations);
        }
        return ResponseEntity.status(type.status()).headers(headers).body(body);
    }

    private static String path(WebRequest request) {
        if (request instanceof NativeWebRequest nativeRequest) {
            HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);
            if (servletRequest != null) {
                return servletRequest.getRequestURI();
            }
        }
        return "/";
    }

    /** Conversion failures (e.g. "EUR" for a currency) must not expose the framework message. */
    private static String messageOf(FieldError error) {
        return error.isBindingFailure()
                ? "valor '%s' possui formato inválido".formatted(error.getRejectedValue())
                : error.getDefaultMessage();
    }

    private static String jsonPath(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : ex.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String describe(MismatchedInputException ex) {
        Class<?> target = ex.getTargetType();
        if (target != null && target.isEnum()) {
            return "valor inválido; valores aceitos: %s".formatted(List.of(target.getEnumConstants()));
        }
        if (ex instanceof InvalidFormatException invalid) {
            return "valor '%s' possui formato inválido".formatted(invalid.getValue());
        }
        return "tipo ou formato inválido";
    }
}
