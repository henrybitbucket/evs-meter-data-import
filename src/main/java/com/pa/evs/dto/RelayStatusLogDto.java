package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.RelayStatusLog;

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
public class RelayStatusLogDto {
	
    private Date createDate;
    
	private String commandSendBy;
	
	private String command;
	
	private String comment;
	
	private String filters;
	
	private String batchUuid;
	
	private Integer totalCount;
	
	private Integer currentCount;
	
	private Integer errorCount;
	
	public RelayStatusLogDto build(RelayStatusLog rl) {
		return builder()
				.createDate(rl.getCreateDate())
				.batchUuid(rl.getBatchUuid())
				.command(rl.getCommand())
				.commandSendBy(rl.getCommandSendBy())
				.comment(rl.getComment())
				.filters(rl.getFilters())
				.totalCount(rl.getTotalCount())
				.currentCount(rl.getCurrentCount())
				.errorCount(rl.getErrorCount())
				.build();
	}
}
