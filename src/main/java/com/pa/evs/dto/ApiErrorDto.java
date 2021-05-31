package com.pa.evs.dto;

import lombok.*;
import org.springframework.http.HttpStatus;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApiErrorDto {
    private HttpStatus status;
    private boolean success;
    private String reason;
}
