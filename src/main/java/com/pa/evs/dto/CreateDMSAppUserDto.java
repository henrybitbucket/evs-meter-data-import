package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pa.evs.model.Users;

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
@JsonIgnoreProperties(ignoreUnknown=true)
public class CreateDMSAppUserDto {

	@Schema(hidden = true)
	private Long id;
	
	@Schema(required = true)
    private String email;
	
    private String fullName;
    private String firstName;
    private String lastName;
    private String avatar;
    private String identification;
    
    @Schema(description = "Phone number", example = "+6500001111", required = true)
    private String phoneNumber;
    
    @Schema(hidden = true)
    private String status;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(required = true)
    private String password;
    
    @JsonIgnore
    @Schema(hidden = true)
    private String hPwd;
    
    @Schema(description = "Require to use OTP to login ", example = "false", required = true)
    private Boolean loginOtpRequire;
    
    @Schema(hidden = true)
    private Boolean firstLoginOtpRequire;
    
    @Schema(hidden = true)
    @JsonIgnore
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
    
    public static CreateDMSAppUserDto build(Users user) {
    	return builder()
    			.avatar(user.getAvatar())
    			.email(user.getEmail())
    			.firstName(user.getFirstName())
    			.fullName(user.getFullName())
    			.id(user.getUserId())
    			.identification(user.getIdentification())
    			.lastName(user.getLastName())
    			.loginOtpRequire(user.getLoginOtpRequire())
    			.phoneNumber(user.getPhoneNumber())
    			.status(user.getStatus())
    			.build();
    }
	
}
