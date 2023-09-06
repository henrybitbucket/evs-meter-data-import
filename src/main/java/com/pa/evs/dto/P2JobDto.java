package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.pa.evs.model.P2Job;

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
public class P2JobDto {
	
	private Long id;
	
	private String name;
	
	private String title;// YYYY-MM-DD HH:mm:ss// first check onboarding
	
	private String jobBy;
	
	@Builder.Default
	private Integer itCount = 0;
	
	private Date createDate;
	
	@Builder.Default
	private List<P2JobDataDto> items = new ArrayList<>();
	
	public static P2JobDto from(P2Job fr) {
		return builder()
				.id(fr.getId())
				.name(fr.getName())
				.jobBy(fr.getJobBy())
				.itCount(fr.getItCount())
				.title(fr.getTitle())
				.createDate(fr.getCreateDate())
				.build();
	}
}
