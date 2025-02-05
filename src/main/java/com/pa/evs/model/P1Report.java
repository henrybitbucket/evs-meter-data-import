package com.pa.evs.model;

import java.sql.Blob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
@Table(
	name = "p1_report", 
	indexes = {
		@Index(columnList = "user_submit", name = "idx_p1_reportt_user_submit")
	}
)
// provisioning step1 report
public class P1Report extends BaseEntity {
	
	@Column(name = "raw_content")
	private Blob rawContent;
	
	@Column(name = "is_latest", columnDefinition = "boolean not null default false")
	private Boolean isLatest;

	@Column(name = "file_name")
	private String fileName;
	
	@Column(name = "user_submit")
	private String userSubmit;
	
	@Column(name = "time_submit")
	private Long timeSubmit;
	
	@Column(name = "comment_submit")
	private String commentSubmit;

}
