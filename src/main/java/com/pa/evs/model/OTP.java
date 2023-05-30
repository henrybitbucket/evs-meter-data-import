package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
@Table(name = "OTP")
public class OTP extends BaseEntity {

	@Column(name = "otp")
	private String otp;
	
	@Column(name = "otp_type")
	private String otpType;

	@Column(name = "action_type")
	private String actionType;
	
	@Column(name = "email")
	private String email;

	@Column(name = "track", length = 1000)
	private String track;
	
	@Column(name = "phone")
	private String phone;
	
	@Column(name = "start_time")
	private Long startTime;
	
	@Column(name = "end_time")
	private Long endTime;
}
