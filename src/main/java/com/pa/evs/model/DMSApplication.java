package com.pa.evs.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
@Table(name = "dms_application")
public class DMSApplication extends BaseEntity {

	@Column(name = "name")
	private String name;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private DMSProject project;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "app")
	@Builder.Default
	private List<DMSApplicationSite> sites = new ArrayList<>();
	
	@Column(name = "created_by", nullable = false)
	private String createdBy;// user phone
	
	@Column(name = "approval_by", nullable = true)
	private String approvalBy;// user email
	
	@Column(name = "reject_by", nullable = true)
	private String rejectBy;// user email
	
	@Column(name = "status")
	private String status;// NEW, APPROVAL, REJECT, DELETED

	@Column(name = "is_guest", columnDefinition = "boolean not null default false")
	@Builder.Default
	private Boolean isGuest = false;
    
}