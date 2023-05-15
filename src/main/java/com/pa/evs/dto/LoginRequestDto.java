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
public class LoginRequestDto {
    private String email;
    private String password;
    @Builder.Default
    private String pf = "OTHER";
}
