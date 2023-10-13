package com.pa.evs.dto;

import org.springframework.web.multipart.MultipartFile;

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
public class UploadFileDto {
	
	private String uid;
	private String type;
	private String originalName;
	private String altName;
	private String description;
	private MultipartFile[] file;
	
}
