package com.pa.evs.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@Table(name = "dms_lock")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class DMSLock extends BaseEntity {
	
	@Column(name = "original_id")
    private String originalId;
	
	@Column(name = "area_id")
    private String areaId;
	
	@Column(name = "lock_bid")
    private String lockBid;
	
	@Column(name = "lock_esn")
    private String lockEsn;
	
	@Column(name = "lock_name")
    private String lockName;
	
	@Column(name = "secret_key")
    private String secretKey;
	
	@Column(name = "lock_number")
    private String lockNumber;
	
	@Column(name = "long", columnDefinition = "decimal")
    private BigDecimal lng;
	
	@Column(name = "lat", columnDefinition = "decimal")
    private BigDecimal lat;
	
	@Column(name = "lastSyncExist")
	@Builder.Default
    private boolean lastSyncExist = true;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor_id")
	private DMSLockVendor vendor;
	
	@Column(name = "battery")
    private String battery;

}
