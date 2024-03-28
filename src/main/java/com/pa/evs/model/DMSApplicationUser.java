package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

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
@Table(name = "dms_application_user")
public class DMSApplicationUser extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "app_id", referencedColumnName = "id")
	private DMSApplication app;
	
	@Column(name = "phone_number")
	private String phoneNumber;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "is_guest", columnDefinition = "boolean not null default false")
	@Builder.Default
	private Boolean isGuest = false;
	
	@Transient
	private Long applicationId;
	
	@Transient
	private Long userId;
	
	private String email;
	
	@Column(name = "h_password")
	private String password;
	
	@Column(name = "is_request_create_new", columnDefinition = "boolean not null default false")
	@Builder.Default
	private Boolean isRequestCreateNew = false;
	
	@Column(name = "is_create_new_success", columnDefinition = "boolean not null default false")
	@Builder.Default
	private Boolean isRequestCreateSuccess = false;
}