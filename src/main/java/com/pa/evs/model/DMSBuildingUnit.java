package com.pa.evs.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "dms_building_unit")
public class DMSBuildingUnit extends BaseEntity {

	public enum BuildingUnitType {
		
		;
		public static BuildingUnitType from(String str) {

			for (BuildingUnitType type : BuildingUnitType.values()) {
				if (type.name().equalsIgnoreCase(str)) {
					return type;
				}
			}
			return null;
		}
	}

	@Column(name = "name")
	private String name;

	@Column(name = "unit")
	private String unit;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "description", columnDefinition="TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private DMSBuildingUnit.BuildingUnitType type;

	@Column(name = "has_tenant")
	private Boolean hasTenant;

	@ManyToOne
	@JoinColumn(name = "floor_level_id")
	private DMSFloorLevel floorLevel;
	
	@Column(name = "remark", length = 500)
	private String remark;
	
	@Column(name = "location_tag", length = 500)
	private String locationTag;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "coupled_date")
    private Date coupledDate;
    
	@Column(name = "full_text")
	private String fullText;
	
	@JsonIgnore
	public void setFullText1(DMSBuildingUnit buildingUnit) {
		DMSFloorLevel floor = buildingUnit.getFloorLevel();
		if (floor == null) {
			return;
		}
		DMSBlock block = floor.getBlock();
		DMSBuilding building = floor.getBuilding();
		if (building == null) {
			return;
		}
		buildingUnit.setFullText((building.getFullText() + "-" + (block == null ? "" : block.getName()) + "-" + (floor == null ? "" : floor.getName()) + "-" + buildingUnit.getName() + "-" + buildingUnit.getRemark()).toLowerCase());
	}

}
