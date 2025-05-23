package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "email_create_access_permission")
public class EmailToCreateAccessPermission extends BaseEntity {

	@Column(name = "message_id")
	private String messageId;
	
	@Column(name = "sender")
	private String sender;
	
	@Column(name = "body", columnDefinition = "TEXT")
	private String body;
	
	@Column(name = "is_processed")
	private Boolean isProcessed;
	
	@Column(name = "status", columnDefinition = "TEXT")
	private String status;
	
	@Column(name = "retry")
	private Integer retry;
	
}
