package com.pa.evs.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pa.evs.dto.GroupDto;
import com.pa.evs.enums.MqttCmdStatus;

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
@Table(name = "log",
	indexes = {
			@Index(columnList = "rls_batch_uuid", name = "idx_rls_batch_uuid_log")
	}
)
public class Log extends BaseEntity {

	private String type;
	
	private Long mid;
	
    private String uid;
    
    private Long oid;

    private Long rmid;
	
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
	
	private String address;
	
	private String topic;
	
	private String ver;
	
	@Column(name = "batch_id")
	private String batchId;
	
	@Column(name = "rep_status")
	private Long repStatus;
	
	@Column(name = "mark_view")
	private Integer markView;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;
	
    @Transient
	private String ftpResStatus;
    
    @Transient
    private String sn;
    
    @Transient
    private GroupDto group;
    
    @Column(name = "cmd_desc")
    private String cmdDesc;
    
    @Column(name = "status_desc")
    private String statusDesc;
    
    @Column(name = "handle_subscribe_desc")
    private String handleSubscribeDesc;
    
	@Column(name = "rls_batch_uuid")
	private String rlsBatchUuid;
	
	private Long timeMDT;
    
    @JsonIgnore
    public String getRepStatusDesc() {
    	return MqttCmdStatus.getDescription(repStatus);
//		if (repStatus == null) return "NO RESPONSE";
//    	return repStatus == 0 ? "OK"
//                : repStatus == 1 ? "Invalid Format"
//                : repStatus == 2 ? "Invalid Command"
//                : repStatus == 3 ? "Invalid Signature"
//                : repStatus == 4 ? "Decryption Failed"
//                : repStatus == 5 ? "Invalid Configuration"
//                : repStatus == 8 ? "Failed send to device (Gateway)"
//                : repStatus == 9 ? "General Error"
//                : "NO RESPONSE";
    }
	
	@SuppressWarnings("unchecked")
	public static Log build(Map<String, Object> data, String type) throws Exception {
		
		Map<String, Object> header = (Map<String, Object>) data.get("header");
		Map<String, Object> payload = (Map<String, Object>) data.get("payload");
		if (header == null) {
			header = new LinkedHashMap<>();
		}
		if (payload == null) {
			payload = new LinkedHashMap<>();
		}
		String pType = payload == null ? "" : (payload.get("type") == null ? payload.get("cmd") + "" : payload.get("type") + "");
		if (StringUtils.isBlank(pType) || "null".equalsIgnoreCase(pType)) {
			pType = (String) data.get("type");
		}
		if (pType == null) {
			pType = "";
		}
		data.remove("type");
		return builder()
				.type(type)
				.mid(header.get("mid") == null ? null : ((Number)(header.get("mid"))).longValue())
				.uid((String)header.get("uid"))
				.oid(header.get("oid") == null ? null : ((Number)(header.get("oid"))).longValue())
				.rmid(header.get("rmid") == null ? null : ((Number)(header.get("rmid"))).longValue())
				.gid((String)header.get("gid"))
				.msn((String)header.get("msn"))
				.sig((String)header.get("sig"))
				.status(header.get("status") == null ? null : ((Number)(header.get("status"))).longValue())
				.pId(payload == null ? null : (payload.get("id") + ""))
				.pType(pType)
				.raw(new ObjectMapper().writeValueAsString(data))
				.repStatus((header.get("mid") != null && "publish".equalsIgnoreCase(type)) ? -999l : null) //-999l PUBLISH status waiting
				.build();
	}
}
