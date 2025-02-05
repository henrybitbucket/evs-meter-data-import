package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "device_project", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "device_id", "type"}) )
public class DeviceProject extends BaseEntity{
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private ProjectTag project;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "device_id")
	private CARequestLog device;
	
	@Column(name = "type", columnDefinition = "varchar not null default 'NA'")
	private String type;// MCU, METER, ADDRESS, NA
}
