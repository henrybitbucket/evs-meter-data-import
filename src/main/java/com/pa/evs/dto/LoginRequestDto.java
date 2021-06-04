package com.pa.evs.dto;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LoginRequestDto {
    private String email;
    private String password;
}
