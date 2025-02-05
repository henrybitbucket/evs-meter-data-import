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
@Table(name = "dms_mc_acc")
public class DMSMcAcc extends BaseEntity {
	
	@Column(name = "password")
	private String password;
	
	@Column(name = "email")
    private String email;

}
