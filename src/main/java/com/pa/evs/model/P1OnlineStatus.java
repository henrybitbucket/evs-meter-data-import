package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pa.evs.enums.DeviceType;

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
@Table(name = "p1_online_status")
public class P1OnlineStatus extends BaseEntity {

	private String uid;
	private String sn;
	private String cid;
	private String msn;
	private String p1Online;
	private String p1OnlineLastUserSent;
	private Long p1OnlineLastSent;
	private Long p1OnlineLastReceived;
	private Boolean isLatest;
	private String version;
	
	@Column(name = "d_type")
	@Enumerated(EnumType.STRING)
	private DeviceType type;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "vendor_id", nullable = false)
	private Vendor vendor;
	
}
