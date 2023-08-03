package com.pa.evs.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.pa.evs.enums.DeviceStatus;
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
@Table(name = "meter_commissioning_report")
public class MeterCommissioningReport extends BaseEntity {
	
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
	
	@Column(name = "meter_photos", length = 20000)
	private String meterPhotos;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private DeviceStatus status;

	@Column(name = "d_type")
	@Enumerated(EnumType.STRING)
	private DeviceType type;

	@Column(name = "last_OBR_date")
	private Long lastOBRDate;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
	@JoinColumn(name = "installer")
	private Users installer;
}
