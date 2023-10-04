package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "relay_status_log", uniqueConstraints = @UniqueConstraint(columnNames = "batch_uuid"))
public class RelayStatusLog extends BaseEntity {

	@Column(name = "command_send_by")
	private String commandSendBy;
	
	@Column(name = "command")
	private String command;
	
	@Column(name = "comment")
	private String comment;
	
	@Column(name = "filters")
	private String filters;
	
	@Column(name = "batch_uuid")
	private String batchUuid;
	
}
