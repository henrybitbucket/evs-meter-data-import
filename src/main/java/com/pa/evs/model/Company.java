package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
@Table(name = "company", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "app_code_id"}))
public class Company extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
    
	@Column(name = "name", nullable = false)
    private String name;
	
	@Column(name = "description")
    private String description;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "app_code_id", columnDefinition = "bigint default 1 not null")
	private AppCode appCode;
}
