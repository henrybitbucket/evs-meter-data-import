package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "pi_log")
public class PiLog extends BaseEntity {

	private String type;
	
	private String msn;
	
	@Column(name = "log_id")
	private Long logId;
	
	@Column(name = "publish_log_id")
	private Long publishLogId;
	
	private Long mid;
	
    @Column(name = "ftp_res_status")
	private String ftpResStatus;
    
	@Column(name = "file_name")
	private String fileName;
	
	@Column(name = "pi_file_name")
	private String piFileName;
	
	@Column(name = "pi_downloaded")
	private Boolean piDownloaded ;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pi_id")
	@JsonIgnore
	private Pi pi;
}
