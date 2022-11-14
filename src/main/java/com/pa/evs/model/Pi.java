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
@Table(name = "pi")
public class Pi extends BaseEntity {

	private String name;
	
	@Column(name = "uuid", unique = true)
	private String uuid;

    private Long lastPing;
    
    private Boolean hide;
    
    private String email;
    
	@Column(name = "iei_id", unique = true)
	private String ieiId;
	
	private String location;
}
