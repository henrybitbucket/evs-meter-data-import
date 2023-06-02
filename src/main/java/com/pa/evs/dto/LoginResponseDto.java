package com.pa.evs.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LoginResponseDto {
    private String token;
    private List<String> authorities;
    @Builder.Default
    private Boolean changePwdRequire = false;
    private String phoneNumber;
    private String email;
}
