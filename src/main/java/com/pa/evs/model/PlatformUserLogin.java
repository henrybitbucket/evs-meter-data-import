package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
