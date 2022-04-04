package com.pa.evs.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class MeterFileDataDto {

    private Long id;
    
    private String filename;
	
	private String ftpResStatus;
	
	private Date createdDate;

}
