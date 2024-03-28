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
public class DMSApplicationUserGuestReqDto {
	private String name;
	private String phone;
	private String email;
	private String password;
	private Boolean createNewUser;
}