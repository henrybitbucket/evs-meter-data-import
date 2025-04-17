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
@Table(name = "dms_user_profile")
public class DMSUserProfile extends BaseEntity {

	@Column(name = "company")
	private String company;

	@Column(name = "days")
	private String days;

	@Column(name = "hours")
	private String hours;

	@Column(name = "email", unique = true)
	private String email;
}
