package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.Vendor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class P1OnlineStatusDto {
	
	private Long id;
	private Date createDate;
	private Date modifyDate;
	private String uid;
	private String sn;
	private String cid;
	private String msn;
	private String p1Online;
	private String p1OnlineLastUserSent;
	private Long p1OnlineLastSent;
	private Long p1OnlineLastReceived;
	private Boolean isLatest;
	private DeviceType type;
	private String version;
	private Vendor vendor;
	
}
