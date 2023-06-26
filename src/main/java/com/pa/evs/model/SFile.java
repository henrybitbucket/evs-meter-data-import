package com.pa.evs.model;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
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
@Table(name = "s_file")
public class SFile extends BaseEntity {
	
	@Column(name = "uid")
	private String uid;
	
	@Column(name = "type")
	private String type;
	
	@Column(name = "original_name")
	private String originalName;
	
	@Column(name = "alt_name")
	private String altName;
	
	@Column(name = "size")
	private long size;

	@Lob
	@Column(name = "content")
	private Blob content;
	
	@Column(name = "description")
	private String description;
	
	@Column(name = "content_type")
	private String contentType;
	
	@Column(name = "created_date")
	private Date createdDate;
	
	@Column(name = "uploaded_by")
	private String uploadedBy;
}
