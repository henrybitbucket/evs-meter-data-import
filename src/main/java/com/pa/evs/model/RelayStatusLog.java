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
	
	@Column(name = "comment", columnDefinition = "text")
	private String comment;
	
	@Column(name = "filters", columnDefinition = "text")
	private String filters;
	
	@Column(name = "batch_uuid")
	private String batchUuid;
	
	@Column(name = "total_count")
	private Integer totalCount;
	
	@Column(name = "current_count")
	private Integer currentCount;
	
	@Column(name = "error_count")
	private Integer errorCount;
	
}
