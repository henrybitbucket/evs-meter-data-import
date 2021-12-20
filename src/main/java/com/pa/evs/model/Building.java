package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "building")
public class Building extends BaseEntity {

	public enum BuildingType {
		;
		public static BuildingType from(String str) {

			for (BuildingType type : BuildingType.values()) {
				if (type.name().equalsIgnoreCase(str)) {
					return type;
				}
			}
			return null;
		}
	}

	@Column(name = "name")
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	private BuildingType type;

	@ManyToOne
	@JoinColumn(name = "address_id")
	private Address address;

	@Column(name = "has_tenant")
	private Boolean hasTenant;

}
