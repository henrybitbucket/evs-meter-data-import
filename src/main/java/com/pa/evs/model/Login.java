package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
@Table(name = "login", uniqueConstraints = @UniqueConstraint(columnNames = "token_id"))
public class Login extends BaseEntity {

	@Column(name = "token_id")
    private String tokenId;
	
	@Column(name = "user_name")
	private String userName;
	
	@Column(name = "start_time")
	private Long startTime;
	
	@Column(name = "end_time")
	private Long endTime;
	
}
