package pe.edu.cibertec.maintenance.dto.common;

import java.time.LocalDateTime;

public class BaseResponseDto {
    private LocalDateTime timestamp;
    private String message;

    public BaseResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}