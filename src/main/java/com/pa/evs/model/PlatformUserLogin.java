package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
@Table(name = "platform_user",  uniqueConstraints = @UniqueConstraint(columnNames = {"email", "name"}))
public class PlatformUserLogin extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
    
	@Column(name = "name")
    private String name;// mobile/ web
	
	@Column(name = "description")
    private String description;// android/ ios/ ....
	
	@Builder.Default
	@Column(name = "active", nullable = false, columnDefinition = "boolean default true")
	private Boolean active = true;
	
	private String email;
	
	@Column(name = "start_time", nullable = false, columnDefinition = "bigint default 0")
	private Long startTime;
	
	@Column(name = "end_time", nullable = false, columnDefinition = "bigint default 4102444800000")
	private Long endTime;
}
