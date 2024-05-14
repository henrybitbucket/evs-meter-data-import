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
@Table(name = "notification_log")
public class NotificationLog extends BaseEntity {

	@Column(name = "action")
	private String action;// SMS, EMAIL
	
	@Column(name = "content", length = 2000)
	private String content;
	
	@Column(name = "type")
	private String type;// SMS, EMAIL

	@Column(name = "receiver")
	private String to;

	@Column(name = "track", length = 1000)
	private String track;
	
	@Column(name = "sid")
	private String sid;// from api

	@Column(name = "created_at")
	private String createdAt;// from api
	
}
