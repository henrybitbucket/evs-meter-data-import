package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
public class DMSProjectDto {
	
	private Long id;

	private String name;//Contract No. 91204683
	
	private String displayName; //ptwp-240315-35678
	
	private Long start;
	
	private Long end;
	
    private Date createDate;

    private Date modifyDate;
    
    private String picUser;
    
    @Builder.Default
    private List<String> subPicUsers = new ArrayList<>();
	
}
