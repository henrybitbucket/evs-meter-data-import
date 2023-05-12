package com.pa.evs.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
@Table(name = "building_unit")
public class BuildingUnit extends BaseEntity {

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
	private BuildingUnit.BuildingUnitType type;

	@Column(name = "has_tenant")
	private Boolean hasTenant;

	@ManyToOne
	@JoinColumn(name = "floor_level_id")
	private FloorLevel floorLevel;
	
	@Column(name = "remark", length = 500)
	private String remark;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "coupled_date")
    private Date coupledDate;
    
	@Column(name = "full_text")
	private String fullText;
	
	@JsonIgnore
	public void setFullText1(BuildingUnit buildingUnit) {
		FloorLevel floor = buildingUnit.getFloorLevel();
		if (floor == null) {
			return;
		}
		Block block = floor.getBlock();
		Building building = floor.getBuilding();
		if (building == null) {
			return;
		}
		buildingUnit.setFullText((building.getFullText() + "-" + (block == null ? "" : block.getName()) + "-" + (floor == null ? "" : floor.getName()) + "-" + buildingUnit.getName() + "-" + buildingUnit.getRemark()).toLowerCase());
	}

}
