package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.Date;
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
	
	private String terminatedBy;
	
	private String status;// NEW, APPROVAL, REJECT, DELETED

	@Builder.Default
	private Boolean isGuest = false;
    
	private DMSTimePeriodDto timePeriod;
	
	@Builder.Default
	private List<Object> users = new ArrayList<>();
	
	private Date createDate;
	
	@SuppressWarnings("rawtypes")
	private List allHis;
	
	private Long timeTerminate;
	
	@Builder.Default
	private Boolean currentUserIsPICUser = false;
}