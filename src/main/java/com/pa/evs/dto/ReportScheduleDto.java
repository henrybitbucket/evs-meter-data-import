package com.pa.evs.dto;

import com.pa.evs.enums.CommandEnum;
import com.pa.evs.enums.JasperFormat;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import com.pa.evs.model.ReportTask;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ReportScheduleDto {

	private Long id;
    
	private Long reportId;

	private Date startTime;
	
	private ReportTask.Type type;
	
	private String parameter;
	
    private JasperFormat format;
	
}
