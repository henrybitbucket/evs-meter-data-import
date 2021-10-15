package com.pa.evs.model;

import com.pa.evs.enums.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "ca_request_log", uniqueConstraints = @UniqueConstraint(columnNames = "uid"))
public class CARequestLog extends BaseEntity {

    private String uid;
    
    private String sn;

	private String cid;
	
    private String msn;

    @Column(name = "status")
	@Enumerated(EnumType.STRING)
	private DeviceStatus status;

    @Column(name = "certificate", length = 20000)
    private String certificate;
    
	@Column(name = "raw", length = 20000)
    private String raw;

	@Column(name = "start_date")
	private Long startDate;
	
	@Column(name = "end_date")
	private Long endDate;
	
	@Column(name = "require_refresh")
	private Boolean requireRefresh;
	
	@Column
	private String ver;

	@Builder.Default
	@Column
	private Long interval = 30L;
	
	@Column(name = "address")
	private String address;
	
	@Column(name = "coupled_datetime")
	private Long coupledDatetime;

	@Column(name = "onboarding_datetime")
	private Long onboardingDatetime;

	@Column(name = "enrollment_datetime")
	private Long enrollmentDatetime;

	@Column(name = "last_subscribe_datetime")
	private Long lastSubscribeDatetime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "installer")
	private Users installer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id")
	private Group group;
	
	@Transient
	private String profile;
	
	public static CARequestLog build(Map<String, Object> data) throws Exception {
		
		return builder()
				.uid((String)data.get("uid"))
				.sn((String)data.get("sn"))
				.cid((String)data.get("cid"))
				.msn((String)data.get("msn"))
				.certificate((String)data.get("pemBase64"))
				.raw((String)data.get("cas"))
				.startDate((Long)data.get("startDate"))
				.endDate((Long)data.get("endDate"))
				.build();
	}
}
