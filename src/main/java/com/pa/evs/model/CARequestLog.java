package com.pa.evs.model;

import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
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

import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @Column(name = "d_type")
	@Enumerated(EnumType.STRING)
	private DeviceType type;

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
	private Long interval = 720L; //publish time

	@Builder.Default
	@Column
	private Long readInterval = 30L; // read interval time
	
	@Column(name = "address")
	private String addressOld;
	
	@Column(name = "home_address")
	private String homeAddress;

	@ManyToOne
	@JoinColumn(name = "building_id")
	private Building building;
	
	@ManyToOne
	@JoinColumn(name = "block_id")
	private Block block;
	
	@ManyToOne
	@JoinColumn(name = "floor_level_id")
	private FloorLevel floorLevel;

	@ManyToOne
	@JoinColumn(name = "building_unit_id")
	private BuildingUnit buildingUnit;

	@ManyToOne
	@JoinColumn(name = "address_id")
	private Address address;
	
	@Column(name = "coupled_datetime")
	private Long coupledDatetime;

	@Column(name = "coupled_user")
	private String coupledUser;
	
	@Column(name = "onboarding_datetime")
	private Long onboardingDatetime;

	@Column(name = "enrollment_datetime")
	private Long enrollmentDatetime;

	@Column(name = "last_subscribe_datetime")
	private Long lastSubscribeDatetime;
	
	@Column(name = "activation_date")
    private Long activationDate;
	
	@Column(name = "deactivation_date")
    private Long deactivationDate;

	@Column(name = "is_ota")
	private Boolean isOta;

	@Column(name = "last_ota_date")
	private Long lastOtaDate;

	@Column(name = "last_mtd_date")
	private Long lastMdtDate;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
	@JoinColumn(name = "installer")
	private Users installer;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
	@JoinColumn(name = "group_id")
	private Group group;
	
	@ManyToOne
	@JoinColumn(name = "vendor_id")
	private Vendor vendor;
	
	@Transient
	private String profile;
	
	private String oldMsn;
	
	@Transient
	private List<String> logs;
	
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
