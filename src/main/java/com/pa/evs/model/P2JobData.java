package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Getter
@Setter
@Builder
@Entity
@Table(
	name = "p2_job_data",
	indexes = {
		@Index(columnList = "job_by", name = "idx_job_by_p2_job_data"),
		@Index(columnList = "job_by,job_name", name = "idx_job_by_job_name_p2_job_data"),
		@Index(columnList = "job_name", name = "idx_job_name_p2_job_data"),
	},
	uniqueConstraints = @UniqueConstraint(columnNames = {"job_by","job_name","it_no"})
)
public class P2JobData extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
	
	@Column(name = "job_name", nullable = false)
	private String jobName;
	
	@Column(name = "job_by", nullable = false)
	private String jobBy;
	
	@Column(name = "job_by_alias", nullable = true)
	private String jobByAlias;
	
	@Column(name = "msn")
	private String msn;
	
	@Column(name = "sn")
	private String sn;
	
	@Column(name = "temp_check_data", columnDefinition = "text")
	private String tmps;
	
	@Column(name = "it_no", columnDefinition = "int default 1 not null")
	private Integer itNo;
}
