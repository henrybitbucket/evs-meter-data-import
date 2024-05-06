package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DMSAccDto {
	
	private Long id;
	private String username;
	private String password;
    private String email;
    private String phoneNumber;
	private DMSProjectDto project;
	
}
