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
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> appCodes;
}
