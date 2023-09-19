package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@Table(name = "device_remove_log")
public class DeviceRemoveLog extends BaseEntity {

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

	@Column(name = "last_act_date")
	private Long lastACTDate;

	@Column(name = "last_OBR_date")
	private Long lastOBRDate;

	@Column(name = "email_installer")
	private String emailInstaller;
	
	@Column(name = "remove_by")
	private String removeBy;

	@Column(name = "vendor_name")
	private String vendorName;
	
	public static DeviceRemoveLog build(CARequestLog dv) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(mapper.writeValueAsString(dv), DeviceRemoveLog.class);
	}
}
