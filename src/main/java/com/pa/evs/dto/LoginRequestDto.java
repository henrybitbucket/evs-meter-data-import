package com.pa.evs.dto;


import io.swagger.annotations.ApiModelProperty;
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
	@ApiModelProperty(hidden = true)
    private String account;
    private String email;
    private String password;
    @Builder.Default
    private String pf = "OTHER";
    @Builder.Default
    private String otp = null;
    
    public String getEmail() {
    	if (email == null) {
    		return account;
    	}
    	return email;
    }
}
