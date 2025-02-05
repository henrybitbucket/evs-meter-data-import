package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "pi")
public class Pi extends BaseEntity {

	private String name;
	
	private String uuid;

    private Long lastPing;
    
    private Boolean hide;
    
    @Column(name = "distribute_flag", columnDefinition = "boolean not null default true")
    @Builder.Default
    private Boolean distributeFlag = true;
    
    private String email;
    
	@Column(name = "iei_id", unique = true)
	private String ieiId;
	
	private String location;
}
