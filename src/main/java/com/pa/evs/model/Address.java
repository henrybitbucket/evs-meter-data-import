package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "address")
public class Address extends BaseEntity {

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "country", length = 500)
	private String country;

	@Column(name = "city", length = 500)
	private String city;

	@Column(name = "town", length = 500)
	private String town;

	@Column(name = "street")
	private String street;

	@Column(name = "street_number")
	private String streetNumber;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "block", length = 500)
	private String block;
	
	@Column(name = "level", length = 500)
	private String level;

	@Column(name = "unit_number", length = 500)
	private String unitNumber;
	
	@Column(name = "remark", length = 500)
	private String remark;
	
	@Column(name = "coupled", length = 10)
	private String coupled;

}
