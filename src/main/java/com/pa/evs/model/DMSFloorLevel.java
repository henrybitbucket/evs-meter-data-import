package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "dms_floor_level")
public class DMSFloorLevel extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "level")
	private String level;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "has_contract")
	private Boolean hasTenant;

	@ManyToOne
	@JoinColumn(name = "building_id")
	private DMSBuilding building;
	
	@ManyToOne
	@JoinColumn(name = "block_id")
	private DMSBlock block;

}
