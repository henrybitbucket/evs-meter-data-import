package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "dms_application_site")
public class DMSApplicationSite extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "app_id")
	private DMSApplication app;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "site_id")
	private DMSSite site;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "work_order_id")
	private DMSWorkOrders workOrder;
	
	@Column(name = "overrideTimePeriod", columnDefinition = "boolean not null default false")
	@Builder.Default
	private boolean overrideTimePeriod = false;
	
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
}