package com.pa.evs.dto;


import com.pa.evs.model.ReportTask;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GetReportTaskResponseDto {
    
    private Long id;
    
    private ReportTask.Type type;

    private Date startTime;
    
    private String fileName; 
    
    private String fileFormat; 
    
}
