package com.pa.evs.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ResetPasswordDto {
    private String password;
    @Builder.Default
    private String token = null;
    @Builder.Default
    private String email = null;
    @Builder.Default
    private String otp = null;
}
