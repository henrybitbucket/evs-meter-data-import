package com.pa.evs.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@Table(name = "device_project", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "device_id"}) )
public class DeviceProject extends BaseEntity{
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private ProjectTag project;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "device_id")
	private CARequestLog device;
}
