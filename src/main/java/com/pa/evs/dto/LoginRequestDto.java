package com.pa.evs.dto;


import io.swagger.v3.oas.annotations.media.Schema;
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
	@Schema(hidden = true)
    private String account;
    private String email;
    private String password;
    @Builder.Default
    private String pf = "OTHER";
    @Builder.Default
    private String otp = null;
    
    public String getEmail() {
    	if (email == null) {
    		return account == null ? null : account.toLowerCase();
    	}
    	return email.toLowerCase();
    }
}
