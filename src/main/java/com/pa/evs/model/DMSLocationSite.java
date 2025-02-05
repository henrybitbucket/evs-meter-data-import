package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
		name = "dms_location_site",
		indexes = {
				@Index(name = "idx_building_2_unit_site_id_dms_location", columnList="building_id,block_id,floor_level_id,building_unit_id,site_id", unique = true),
				@Index(name = "idx_location_key_site_id_dms_location", columnList="location_key,site_id", unique = true),
				@Index(name = "idx_location_key_dms_location_site", columnList="location_key", unique = false)
				
		}
)
public class DMSLocationSite extends BaseEntity {

	
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
	@JoinColumn(name = "site_id")
	private DMSSite site;

}
