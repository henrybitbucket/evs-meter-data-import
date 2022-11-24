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
@Table(name = "setting")
public class Setting extends BaseEntity {

	@Column(name = "s_key", length = 5000)
	private String key;

	@Column(name = "s_value", length = 50000)
	private String value;

	@Column(name = "s_order")
	private Integer order;
	
	@Column(name = "status")
	private String status;

}
