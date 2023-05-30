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
@Table(name = "Token")
public class Token extends BaseEntity {

	@Column(name = "type")
	private String type;

	@Column(name = "token")
	private String token;
	
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
