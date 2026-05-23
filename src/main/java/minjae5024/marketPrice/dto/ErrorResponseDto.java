package minjae5024.marketPrice.dto;

import lombok.Getter;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ErrorResponseDto {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;
    private final List<FieldErrorDetail> fieldErrors;

    public ErrorResponseDto(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = new ArrayList<>();
    }

    public ErrorResponseDto(int status, String error, String message, List<FieldError> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors == null ? new ArrayList<>() : fieldErrors.stream()
                .map(FieldErrorDetail::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class FieldErrorDetail {
        private final String field;
        private final String value;
        private final String reason;

        public FieldErrorDetail(FieldError fieldError) {
            this.field = fieldError.getField();
            this.value = fieldError.getRejectedValue() == null ? "" : fieldError.getRejectedValue().toString();
            this.reason = fieldError.getDefaultMessage();
        }
    }
}
