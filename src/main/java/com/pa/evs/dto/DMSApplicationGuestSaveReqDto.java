package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
public class DMSApplicationGuestSaveReqDto extends DMSApplicationSaveReqDto {

	private Long projectId;
	
	@Schema(hidden = true)
	@JsonIgnore
	private String submittedBy;
	
	@Builder.Default
	private List<DMSApplicationSiteItemReqDto> sites = new ArrayList<>();
	
	@Builder.Default
	private List<String> userPhones = new ArrayList<>();
	
	@Builder.Default
	private List<DMSApplicationUserGuestReqDto> guests = new ArrayList<>();
	
//	// for create new user
//	private boolean createNewUser;
//	
//	private String email;
//	
//	private String firstName;
//	
//	private String lastName;
//	
//	private String password;
}
