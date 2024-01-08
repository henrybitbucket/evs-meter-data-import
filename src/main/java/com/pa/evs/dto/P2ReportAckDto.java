package com.pa.evs.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class P2ReportAckDto {

	private Long id;
	private String uid;
	private String sn;
	private String cid;
	private String msn;
	private Boolean isPassed;
    private String kwh;
    private String kw;
    private String i;
    private String v;
    private String pf;
	private Long dt;
	private String meterPhotos;
	private DeviceStatus status;
	private DeviceType type;
	private Long lastOBRDate;
	private Long installer;
    private String installerName;
    private String installerEmail;
	private String p1Online;
	private String p1OnlineLastUserSent;
	private Long p1OnlineLastSent;
	private Long p1OnlineLastReceived;
	private String userSubmit;
	private Long timeSubmit;
	private String coupledUser;
	private Long countDevicesSubmitByUser;
	private Date createDate;
	private String commentSubmit;
	private String jobSheetNo;
	private String jobBy;
	private String managerSubmit;
	private String contractOrder;
	
}
