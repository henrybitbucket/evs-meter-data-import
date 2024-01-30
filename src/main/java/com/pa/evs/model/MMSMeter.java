package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
@Table(name = "mms_meter", uniqueConstraints = {@UniqueConstraint(columnNames = "uid"), @UniqueConstraint(columnNames = "msn")})
public class MMSMeter extends BaseEntity {

    private String uid; // MCU UID
    
    @Column(name = "msn", unique = true, nullable = false)
    private String msn;

    private String lastUid; // LAST MCU UID
    
    private String remark;
    
    private Long lastestDecoupleTime;
    
    private Long lastestCoupledTime;
    
    private String lastestDecoupleUser;
    
    private String lastestCoupledUser;
    
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

	@Column(name = "home_address")
	private String homeAddress;
}
