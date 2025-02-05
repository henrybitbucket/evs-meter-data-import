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
import jakarta.persistence.UniqueConstraint;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "dms_block", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "building_id"})})
public class DMSBlock extends BaseEntity {

	@Column(name = "name")
	private String name;

	@Column(name = "block")
	private String block;

	@Column(name = "display_name")
	private String displayName;
	
	@Column(name = "has_contract")
	private Boolean hasTenant;

	@ManyToOne
	@JoinColumn(name = "building_id")
	private DMSBuilding building;

}
