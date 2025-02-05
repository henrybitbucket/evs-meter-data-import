package com.pa.evs.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "dms_application")
public class DMSApplication extends BaseEntity {

	@Column(name = "name")
	private String name;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private DMSProject project;
	
	@Column(name = "project_name")
	private String projectName;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "app")
	@Builder.Default
	private List<DMSApplicationSite> sites = new ArrayList<>();
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "app")
	@Builder.Default
	private List<DMSApplicationUser> users = new ArrayList<>();
	
	@Column(name = "created_by", nullable = false)
	private String createdBy;// user phone
	
	@Column(name = "approval_by", nullable = true)
	private String approvalBy;// user email
	
	@Column(name = "reject_by", nullable = true)
	private String rejectBy;// user email
	
	@Column(name = "terminated_by", nullable = true)
	private String terminatedBy;// user email
	
	@Column(name = "status")
	private String status;// NEW, APPROVAL, REJECT, DELETED

	@Column(name = "is_guest", columnDefinition = "boolean not null default false")
	@Builder.Default
	private Boolean isGuest = false;
	
	@Column(name = "time_terminate", nullable = true)
	private Long timeTerminate;
	
	// timePeriod
	// timePeriod dates in year
	@Builder.Default
	@Column(name = "timePeriodDatesIsAlways", columnDefinition = "boolean not null default false")
	private boolean timePeriodDatesIsAlways = false;
	private Long timePeriodDatesStart;
	private Long timePeriodDatesEnd;
	// end timePeriod dates in year
	
	// timePeriod days in week
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsAlways", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsAlways = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsMon", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsMon = false;
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsTue", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsTue = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsWed", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsWed = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsThu", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsThu = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsFri", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsFri = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsSat", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsSat = false;
	
	@Builder.Default
	@Column(name = "timePeriodDayInWeeksIsSun", columnDefinition = "boolean not null default false")
	private boolean timePeriodDayInWeeksIsSun = false;
	// end timePeriod days in week
	
	// timePeriod time in day
	@Builder.Default
	@Column(name = "timePeriodTimeInDayIsAlways", columnDefinition = "boolean not null default false")
	private boolean timePeriodTimeInDayIsAlways = false;
	private Integer timePeriodTimeInDayHourStart;
	private Integer timePeriodTimeInDayHourEnd;
	private Integer timePeriodTimeInDayMinuteStart;
	private Integer timePeriodTimeInDayMinuteEnd;
	
	@Column(name = "guest_token_id", nullable = true, columnDefinition = "TEXT")
	private String guestTokenId;
	
	@Column(name = "guest_token_start_time", nullable = true)
	private Long guestTokenStartTime;
	
	@Column(name = "guest_token_end_time", nullable = true)
	private Long guestTokenEndTime;
    
}