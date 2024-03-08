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
@Table(name = "menu_items")
public class MenuItems extends BaseEntity {
	
	@Column(name = "app_code")
	private String appCode;

	@Column(name = "items", columnDefinition="TEXT")
	private String items;
	
}
