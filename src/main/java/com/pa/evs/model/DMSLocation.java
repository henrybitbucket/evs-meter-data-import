package com.pa.evs.model;

import jakarta.persistence.Entity;
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
		name = "dms_location",
		indexes = {
				@Index(name = "idx_building_2_unit_dms_location", columnList="building_id,block_id,floor_level_id,building_unit_id", unique = true)
		}
)
public class DMSLocation extends BaseEntity {

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
	@JoinColumn(name = "building_unit_id")
	private DMSBuildingUnit buildingUnit;
	
	@ManyToOne
	@JoinColumn(name = "address_id")
	private DMSAddress address;

}
