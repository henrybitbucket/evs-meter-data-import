package com.pa.evs.dto;


import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
	
	private String reportId;
	
	private Long reportTaskId;
    
    private String fileName;
    
    private String fileFormat;
    
    private Date createDate;
   
}
