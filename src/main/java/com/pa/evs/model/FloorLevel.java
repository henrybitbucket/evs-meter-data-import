package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "floor_level")
public class FloorLevel extends BaseEntity {

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
	private Building building;

}
