package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
@Table(name = "dms_site")
public class DMSSite extends BaseEntity {

	@Column(name = "label", unique = true)
	private String label;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "remark", length = 500)
	private String remark;

	@Column(name = "radius", length = 500)
	private String radius;

}
