package com.pa.evs.dto;

import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ScheduleDto {

	private Long id;
    
	private Long groupId;

	private Date startTime;
	
	private GroupTask.Type type;

	private CommandEnum command;
	
}
