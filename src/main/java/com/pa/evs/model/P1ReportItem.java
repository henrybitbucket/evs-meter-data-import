package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
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
@Table(
	name = "p1_report_item", 
	indexes = {
		@Index(columnList = "user_submit", name = "idx_p1_reportt_user_submit"),
		@Index(columnList = "user_submit,sn", name = "idx_p1_reportt_user_submit_sn")
	}
)
// provisioning step1 report
public class P1ReportItem extends BaseEntity {
	
	private String uid;
	private String sn;
	private String msn;
	
	@Column(name = "raw_content", columnDefinition = "TEXT")
	private String rawContent;
	
	@Column(name = "is_latest", columnDefinition = "boolean not null default false")
	private Boolean isLatest;

	@Column(name = "user_submit")
	private String userSubmit;
	
	@Column(name = "time_submit")
	private Long timeSubmit;
	
	@Column(name = "comment_submit")
	private String commentSubmit;
	
	@ManyToOne
	@JoinColumn(name = "p1_report_id")
	private P1Report p1Report;

}
