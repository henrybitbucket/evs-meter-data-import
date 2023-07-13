package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.SFile;

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
public class SFileDto {
	
	private Long id;
	
	private String uid;
	
	private String type;
	
	private String originalName;
	
	private String altName;
	
	private long size;

	private String description;
	
	private String contentType;
	
	private Date createdDate;
	
	private String uploadedBy;
	
	public static SFileDto from (SFile fr) {
		return builder()
				.id(fr.getId())
				.uid(fr.getUid())
				.type(fr.getType())
				.originalName(fr.getOriginalName())
				.altName(fr.getAltName())
				.size(fr.getSize())
				.description(fr.getDescription())
				.contentType(fr.getContentType())
				.createdDate(fr.getCreateDate())
				.uploadedBy(fr.getUploadedBy())
				.build();
	}
}