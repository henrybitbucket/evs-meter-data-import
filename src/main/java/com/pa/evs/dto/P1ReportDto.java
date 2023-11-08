package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.P1Report;

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
public class P1ReportDto {
	
	private Long id;
	
	private String rawContent;
	
	private Boolean isLatest;

	private String userSubmit;
	
	private String fileName;
	
	private Long timeSubmit;
	
	private String commentSubmit;
	
	private Date createDate;
	
	public static P1ReportDto from(P1Report fr) {
		return builder()
				.id(fr.getId())
				.fileName(fr.getFileName())
				.userSubmit(fr.getUserSubmit())
				.timeSubmit(fr.getTimeSubmit())
				.build();
	}
}
