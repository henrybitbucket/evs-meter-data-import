package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pa.evs.dto.BuildingDto;

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
@Table(name = "dms_building")
public class DMSBuilding extends BaseEntity {

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
	private DMSAddress address;

	@Column(name = "has_tenant")
	private Boolean hasTenant;

	@Column(name = "full_text")
	private String fullText;
	
	@JsonIgnore
	public void setFullText1(DMSBuilding bd) {
		String fullText = bd.getName() 
				+ '-' + bd.getAddress().getBlock()
				+ '-' + bd.getAddress().getLevel()
				+ '-' + bd.getAddress().getUnitNumber()
				+ '-' + bd.getAddress().getStreet() 
				+ '-' + bd.getAddress().getPostalCode() 
				+ '-' + bd.getAddress().getCity();
		setFullText(fullText.toLowerCase());
	}
	
	@JsonIgnore
	public void setFullText1(BuildingDto dto) {
		String fullText = dto.getName() 
				+ '-' + dto.getAddress().getBlock()
				+ '-' + dto.getAddress().getLevel()
				+ '-' + dto.getAddress().getUnitNumber()
				+ '-' + dto.getAddress().getStreet() 
				+ '-' + dto.getAddress().getPostalCode() 
				+ '-' + dto.getAddress().getCity();
		setFullText(fullText.toLowerCase());
	}
}
