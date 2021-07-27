package com.pa.evs.model;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;

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
@Table(name = "log")
public class Log extends BaseEntity {

	private String type;
	
	private Long mid;
	
    private String uid;
    
    private Long oid;
	
    private String gid;
	
    private String msn;
	
    private String sig;
    
    private Long status;
	
	@Column(name = "p_id")
    private String pId;
	
	@Column(name = "p_type")
    private String pType;
	
	@Column(name = "raw", length = 20000)
    private String raw;
	
	@Column(name = "mqtt_address")
	private String mqttAddress;
	
	private String topic;
	
	@SuppressWarnings("unchecked")
	public static Log build(Map<String, Object> data, String type) throws Exception {
		
		Map<String, Object> header = (Map<String, Object>) data.get("header");
		Map<String, Object> payload = (Map<String, Object>) data.get("payload");
		
		return builder()
				.type(type)
				.mid(header.get("mid") == null ? null : ((Number)(header.get("mid"))).longValue())
				.uid((String)header.get("uid"))
				.oid(header.get("oid") == null ? null : ((Number)(header.get("oid"))).longValue())
				.gid((String)header.get("gid"))
				.msn((String)header.get("msn"))
				.sig((String)header.get("sig"))
				.status(header.get("status") == null ? null : ((Number)(header.get("status"))).longValue())
				.pId(payload == null ? null : (payload.get("id") + ""))
				.pType(payload == null ? null : (payload.get("type") + ""))
				.raw(new ObjectMapper().writeValueAsString(data))
				.topic((String)data.get("topic"))
				.build();
	}
}
