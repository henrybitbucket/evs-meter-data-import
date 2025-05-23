package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "locks_access_permission")
public class LocksAccessPermisison extends BaseEntity {

	@Column(name = "full_name")
    private String fullName;
    
	@Column(name = "phone_number")
    private String phoneNumber;

	@Column(name = "group_name")
    private String groupName;
    
	@Column(name = "start_date")
    private String startDate;
    
	@Column(name = "end_date")
    private String endDate;
	
	@Column(name = "start_hour")
    private Integer startHour;
    
	@Column(name = "start_minute")
    private Integer startMinute;
    
	@Column(name = "end_hour")
    private Integer endHour;
    
	@Column(name = "end_minute")
    private Integer endMinute;
    
	@Column(name = "email")
    private String email;
	
	@Column(name = "remarks")
    private String remarks;
    
	@Column(name = "url")
    private String url;
	
	@Column(name = "message_id")
	private String messageId;
}
