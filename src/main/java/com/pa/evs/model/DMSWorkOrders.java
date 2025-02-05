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
@Table(
		name = "dms_work_orders"
)
public class DMSWorkOrders extends BaseEntity {

	private String name;
	
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "group_user_id")
//	private GroupUser group;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "site_id", nullable = true)
	private DMSSite site;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "appication_id", nullable = true)
	private DMSApplication app;
	
	@Column(name = "status")
	private String status;

	// timePeriod dates in year
	@Builder.Default
	private boolean timePeriodDatesIsAlways = false;
	private Long timePeriodDatesStart;
	private Long timePeriodDatesEnd;
	// end timePeriod dates in year
	
	// timePeriod days in week
	@Builder.Default
	private boolean timePeriodDayInWeeksIsAlways = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsMon = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsTue = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsWed = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsThu = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsFri = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsSat = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsSun = false;
	// end timePeriod days in week
	
	// timePeriod time in day
	@Builder.Default
	private boolean timePeriodTimeInDayIsAlways = false;
	@Builder.Default
	private Integer timePeriodTimeInDayHourStart = 0;
	@Builder.Default
	private Integer timePeriodTimeInDayHourEnd = 0;
	@Builder.Default
	private Integer timePeriodTimeInDayMinuteStart = 0;
	@Builder.Default
	private Integer timePeriodTimeInDayMinuteEnd = 0;
}
