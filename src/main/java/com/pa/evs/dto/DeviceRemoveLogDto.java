package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.DeviceRemoveLog;

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
public class DeviceRemoveLogDto {

	private Date createDate;
	
    private String uid;
    
    private String sn;

	private String cid;
	
    private String msn;

	private DeviceStatus status;
    
	private DeviceType type;

    private String certificate;
    
    private String raw;

	private Long startDate;
	
	private Long endDate;
	
	private Boolean requireRefresh;
	
	private String ver;

	private Long interval;

	private Long readInterval;
	
	private String addressOld;
	
	private String homeAddress;
	
	private Long coupledDatetime;

	private String coupledUser;
	
	private Long onboardingDatetime;

	private Long enrollmentDatetime;

	private Long lastSubscribeDatetime;
	
    private Long activationDate;
	
    private Long deactivationDate;

	private Boolean isOta;

	private Long lastOtaDate;

	private Long lastMdtDate;

	private Long lastACTDate;

	private Long lastOBRDate;

	private String emailInstaller;
	
	private String operationBy;
	
	private String operation;

	private String vendorName;
	
	private String reason;
	
	private String remark;
	
	private AddressLogDto address;
	
	public static DeviceRemoveLogDto build(DeviceRemoveLog dv) throws Exception {

		return builder()
		.activationDate(dv.getActivationDate())
		.addressOld(dv.getAddressOld())
		.address(dv.getAddressLog() != null ? AddressLogDto.build(dv.getAddressLog()) : null)
		.createDate(dv.getCreateDate())
		.certificate(dv.getCertificate())
		.cid(dv.getCid())
		.coupledDatetime(dv.getCoupledDatetime())
		.coupledUser(dv.getCoupledUser())
		.deactivationDate(dv.getDeactivationDate())
		.emailInstaller(dv.getEmailInstaller())
		.endDate(dv.getEndDate())
		.enrollmentDatetime(dv.getEnrollmentDatetime())
		.homeAddress(dv.getHomeAddress())
		.interval(dv.getInterval())
		.isOta(dv.getIsOta())
		.lastACTDate(dv.getLastACTDate())
		.lastMdtDate(dv.getLastMdtDate())
		.lastOBRDate(dv.getLastOBRDate())
		.lastOtaDate(dv.getLastOtaDate())
		.lastSubscribeDatetime(dv.getLastSubscribeDatetime())
		.msn(dv.getMsn())
		.onboardingDatetime(dv.getOnboardingDatetime())
		.raw(dv.getRaw())
		.readInterval(dv.getReadInterval())
		.remark(dv.getRemark())
		.reason(dv.getReason())
		.operation(dv.getOperation())
		.operationBy(dv.getOperationBy())
		.requireRefresh(dv.getRequireRefresh())
		.sn(dv.getSn())
		.startDate(dv.getStartDate())
		.status(dv.getStatus())
		.type(dv.getType())
		.uid(dv.getUid())
		.vendorName(dv.getVendorName())
		.ver(dv.getVer())
		.build();
	}
}
