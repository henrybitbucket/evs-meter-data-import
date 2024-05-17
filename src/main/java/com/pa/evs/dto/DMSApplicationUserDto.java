package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DMSApplicationUserDto {

	private Long id;
	private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
	
}
