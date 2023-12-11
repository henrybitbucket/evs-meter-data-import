package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.P1ReportItem;

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
public class P1ReportItemDto {
	
	private Long id;
	
	private String rawContent;
	
	private Boolean isLatest;

	private String userSubmit;
	
	private Long timeSubmit;
	
	private String commentSubmit;
	
	private Date createDate;
	
	private String sn;
	
	private String summaryTestResult;
	
	public static P1ReportItemDto from(P1ReportItem fr) {
		return builder()
				.id(fr.getId())
				.sn(fr.getSn())
				.userSubmit(fr.getUserSubmit())
				.timeSubmit(fr.getTimeSubmit())
				.summaryTestResult(fr.getSummaryTestResult())
				.rawContent(fr.getRawContent())
				.build();
	}
}
