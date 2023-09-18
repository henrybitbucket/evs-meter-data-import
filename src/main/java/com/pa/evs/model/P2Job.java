package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

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
	name = "p2_job",
	indexes = {
		@Index(columnList = "job_by", name = "idx_job_by_p2_job"),
		@Index(columnList = "name", name = "idx_name_p2_job"),
	},
	uniqueConstraints = @UniqueConstraint(columnNames={"job_by", "name"})
)
public class P2Job extends Base1Entity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
	
	@Column(name = "name")
	private String name;// YYYYMM0001
	
	// https://powerautomationsg.atlassian.net/browse/MMS-143
	private String title;// YYYY-MM-DD HH:mm:ss// first check onboarding
	
	@Column(name = "job_by")
	private String jobBy;
	
	@Column(name = "job_by_alias", nullable = true)
	private String jobByAlias;
	
	@Column(name = "it_count", columnDefinition = "int default 0 not null")
	private Integer itCount;
	
	@Version
	private long version;
	
	@Column(name = "user_submit")
	private String userSubmit;
	
	@Column(name = "time_submit")
	private Long timeSubmit;
	
	@Column(name = "comment_submit")
	private String commentSubmit;
}
