package adminServer.mvp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).orElse("Requisição inválida");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message); detail.setTitle("Requisição inválida"); return detail;
    }
    @ExceptionHandler(java.util.NoSuchElementException.class)
    ProblemDetail notFound() { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado"); }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(RuntimeException exception) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict() { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "O recurso já existe ou viola uma restrição"); }
    @ExceptionHandler(adminServer.mvp.auth.InvalidCredentialsException.class)
    ProblemDetail unauthorized() { return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"); }
}
