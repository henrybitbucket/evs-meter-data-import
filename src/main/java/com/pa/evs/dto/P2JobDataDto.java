package com.pa.evs.dto;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.pa.evs.model.P2JobData;

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
public class P2JobDataDto {

	private Long id;
	
	private String jobName;
	
	private String jobBy;
	
	private String msn;
	
	private String sn;
	
	private Integer itNo;
	
	private Date createDate;
	
	@Builder.Default
	private Map<String, Object> tempDataChecks = new LinkedHashMap<>();
	
	public static P2JobDataDto from(P2JobData fr) {
		return builder()
				.id(fr.getId())
				.jobName(fr.getJobName())
				.jobBy(fr.getJobBy())
				.msn(fr.getMsn())
				.sn(fr.getSn())
				.itNo(fr.getItNo())
				.createDate(fr.getCreateDate())
				.build();
	}
}
