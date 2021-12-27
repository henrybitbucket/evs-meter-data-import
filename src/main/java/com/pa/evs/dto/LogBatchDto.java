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
public class LogBatchDto  {

	private long id;
	
	private String uuid;

    private String email;
    
    private Date createDate;
    
	private long userId;
	
	private String userName;
	
	private String userEmail;
}
