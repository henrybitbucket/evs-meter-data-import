package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
