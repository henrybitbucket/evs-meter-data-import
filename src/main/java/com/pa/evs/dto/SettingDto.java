package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.Setting;

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
public class SettingDto {

	private Long id;
	
	private String key;

	private String value;

	private Integer order;
	
	private String status;
	
    private Date createDate;

    private Date modifyDate;
    
    public static SettingDto from(Setting fr) {
    	
    	return builder()
    			.id(fr.getId())
    			.key(fr.getKey())
    			.value(fr.getValue())
    			.order(fr.getOrder())
    			.status(fr.getStatus())
    			.build();
    }
}
