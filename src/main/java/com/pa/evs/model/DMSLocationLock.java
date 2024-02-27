package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
@Table(
		name = "dms_location_lock",
		indexes = {
				@Index(name = "idx_building_2_unit_lock_id_dms_location_lock", columnList="building_id,block_id,floor_level_id,building_unit_id,lock_id", unique = true),
				@Index(name = "idx_location_key_lock_id_dms_location_lock", columnList="location_key,lock_id", unique = true),
				@Index(name = "idx_lock_id_dms_location_lock", columnList="lock_id", unique = true),
				@Index(name = "idx_location_key_dms_location_lock", columnList="location_key", unique = false)
				
		}
)
public class DMSLocationLock extends BaseEntity {

	
	@Column(name = "location_key")
	private String locationKey;// location key to get location lock
	
    @ManyToOne
	@JoinColumn(name = "building_id")
	private DMSBuilding building;
	
	@ManyToOne
	@JoinColumn(name = "block_id")
	private DMSBlock block;
	
	@ManyToOne
	@JoinColumn(name = "floor_level_id")
	private DMSFloorLevel floorLevel;

	@ManyToOne
	@JoinColumn(name = "building_unit_id", nullable = false)
	private DMSBuildingUnit buildingUnit;
	
	@ManyToOne
	@JoinColumn(name = "address_id")
	private DMSAddress address;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lock_id")
	private DMSLock lock;

}
