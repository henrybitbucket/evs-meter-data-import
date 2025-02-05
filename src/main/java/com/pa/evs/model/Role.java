package com.pa.evs.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
@Table(name = "role")
public class Role extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
    
	@Column(name = "r_name", unique = true)
    private String name;
	
	@Column(name = "r_desc")
    private String desc;
	
	@OneToMany(mappedBy = "role")
    private List<UserRole> roles;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "app_code_id", columnDefinition = "bigint default 1 not null")
	private AppCode appCode;
}
