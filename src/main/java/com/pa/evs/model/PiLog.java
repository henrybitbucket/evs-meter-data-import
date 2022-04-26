package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "pi_log")
public class PiLog extends BaseEntity {

	private String type;
	
	private String msn;
	
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
	private Pi pi;
}
