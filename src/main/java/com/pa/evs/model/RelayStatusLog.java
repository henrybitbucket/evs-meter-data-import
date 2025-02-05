package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
@Table(
		name = "relay_status_log", 
		uniqueConstraints = @UniqueConstraint(columnNames = "batch_uuid"), 
		indexes = {
			@Index(columnList = "batch_uuid", name = "idx_relay_status_log_relay_status_log")
		}
)
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
	
	@Column(name = "mid")
	private Long mid;
	
	@Column(name = "uid")
	private String uid;
	
}
