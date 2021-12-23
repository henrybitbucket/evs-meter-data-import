package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.enums.CommandEnum;
import com.pa.evs.model.GroupTask.Type;

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
public class GroupTaskDto {

    private Long id;
    
    private CommandEnum command;

    private Type type;
    
    private String groupName;

    private Long groupId;

    private Date startTime;
    
	private Long userId;
}
