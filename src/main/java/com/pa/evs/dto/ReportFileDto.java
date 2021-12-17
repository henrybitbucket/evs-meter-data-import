package com.pa.evs.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ReportFileDto {

	private Long id;
    
    private String fileName;
    
    private String fileFormat;
	
}
