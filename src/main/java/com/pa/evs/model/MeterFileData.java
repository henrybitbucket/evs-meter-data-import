package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
@Table(name = "meter_file_data")
public class MeterFileData extends BaseEntity {

	private String filename;
	
    @Column(name = "ftp_res_status")
	private String ftpResStatus;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pi_id")
	private Pi pi;
}
