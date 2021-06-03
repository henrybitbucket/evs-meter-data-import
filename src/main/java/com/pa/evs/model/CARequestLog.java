package com.pa.evs.model;

import java.util.Map;

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
@Getter
@Setter
@Builder
@Entity
@Table(name = "ca_request_log", uniqueConstraints = @UniqueConstraint(columnNames = "uid"))
public class CARequestLog extends BaseEntity {

    private String uid;
	
    private String msn;
	
    @Column(name = "certificate", length = 20000)
    private String certificate;
    
	@Column(name = "raw", length = 20000)
    private String raw;
	
	@Column(name = "start_date")
	private Long startDate;
	
	@Column(name = "end_date")
	private Long endDate;
	
	@Column(name = "require_refresh")
	private Boolean requireRefresh;
	
	public static CARequestLog build(Map<String, Object> data) throws Exception {
		
		return builder()
				.uid((String)data.get("uid"))
				.msn((String)data.get("msn"))
				.certificate((String)data.get("pemBase64"))
				.raw((String)data.get("cas"))
				.startDate((Long)data.get("startDate"))
				.endDate((Long)data.get("endDate"))
				.build();
	}
}
