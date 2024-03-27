package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

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
public class DMSApplicationDto {

	private Long id;
	
	private String name;
	
	private DMSProjectDto project;
	
	@Builder.Default
	private List<Object> sites = new ArrayList<>();
	
	private String createdBy;// user phone
	
	private String approvalBy;// user email
	
	private String rejectBy;// user email
	
	private String status;// NEW, APPROVAL, REJECT, DELETED

	@Builder.Default
	private Boolean isGuest = false;
    
}