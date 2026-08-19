package adminServer.mvp.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).orElse("Requisição inválida");
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida", message, request);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso não encontrado", "Recurso não encontrado", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "Requisição inválida" : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida", message, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Conflito de integridade de dados",
                "O recurso já existe ou viola uma restrição", request);
    }

    @ExceptionHandler(adminServer.mvp.auth.InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> unauthorized(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Não autorizado", "Credenciais inválidas", request);
    }

    @ExceptionHandler(adminServer.mvp.auth.TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> tooManyRequests(adminServer.mvp.auth.TooManyRequestsException exception,
            HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.TOO_MANY_REQUESTS, "Limite de tentativas excedido",
                "Muitas tentativas. Tente novamente mais tarde.", request);
        return responseBuilder(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(detail);
    }

    private ResponseEntity.BodyBuilder responseBuilder(HttpStatus status) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String message,
            HttpServletRequest request) {
        return responseBuilder(status).body(problem(status, title, message, request));
    }

    private ProblemDetail problem(HttpStatus status, String title, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create("about:blank"));
        detail.setTitle(title);
        if (request != null) {
            detail.setInstance(URI.create(request.getRequestURI()));
        }
        return detail;
    }
}
