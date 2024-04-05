package com.pa.evs.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pa.evs.dto.ProjectTagDto;
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
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
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

	@Column(name = "last_act_date")
	private Long lastACTDate;

	@Column(name = "last_OBR_date")
	private Long lastOBRDate;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
	@JoinColumn(name = "installer")
	private Users installer;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
	@JoinColumn(name = "group_id")
	private Group group;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "vendor_id", nullable = false)
	private Vendor vendor;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy = "device", cascade = CascadeType.MERGE)
    private List<DeviceProject> deviceProject;

	@OneToMany(fetch=FetchType.LAZY, mappedBy = "device", cascade = CascadeType.MERGE)
	@Builder.Default
    private Set<DeviceIEINode> deviceIEINodes = new HashSet<>();
	
	@Transient
	@Builder.Default
	private List<ProjectTagDto> projectTags = new ArrayList<>();
	
	@Transient
	@Builder.Default
	private List<ProjectTagDto> mcuProjectTags = new ArrayList<>();
	
	@Transient
	@Builder.Default
	private List<ProjectTagDto> meterProjectTags = new ArrayList<>();
	
	@Transient
	@Builder.Default
	private List<ProjectTagDto> addressProjectTags = new ArrayList<>();
	
	@Transient
	private String profile;
	
	private String oldMsn;
	
	private String p1Online;
	
	private String p1OnlineLastUserSent;
	
	private Long p1OnlineLastSent;
	
	private Long p1OnlineLastReceived;
	
	@Column(name = "last_meter_commissioning_report")
	private Date lastMeterCommissioningReport;
	
	@Column(name = "last_meter_commissioning_report_ack")
	private Date lastMeterCommissioningReportAck;
	
	@Column(name = "latestINFFirmwaveRequest", length = 255)
	private String latestINFFirmwaveRequest;//time_INF_version
	
	@Column(name = "device_csr_Signature_Algorithm")
	private String deviceCsrSignatureAlgorithm;
	
	@Column(name = "device_key_type")
	private String deviceKeyType;
	
	@Column(name = "is_replaced")
	private Boolean isReplaced;

	@Column(name = "old_sn", columnDefinition="TEXT")
	private String oldSn;
	
	@Column(name = "replace_reason", columnDefinition="TEXT")
	private String replaceReason;
	
	@Transient
	private List<String> logs;
	
	@Transient
	private DeviceType typeP2;
	
	@Transient
	private DeviceType typeP3;
	
	@Builder.Default
	@Column(name = "send_mdt_to_pi", columnDefinition = "int default 1 not null")
	private Integer sendMDTToPi = 2;//1 - to send data to the pi, 2 - do not send data to the pi
	
	private String remark;
	
	@Column(name = "remark_mcu", length = 500)
	private String remarkMCU;
	
	@Column(name = "remark_meter", length = 500)
	private String remarkMeter;
	
	@Transient
    private Long lastestDecoupleTime;
    
	@Transient
    private String lastestDecoupleUser;

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
