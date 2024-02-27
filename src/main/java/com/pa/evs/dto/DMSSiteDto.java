package com.pa.evs.dto;

import java.math.BigDecimal;
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
public class DMSSiteDto {

	private Long id;
	
	private String label;

	private String description;

	private String remark;

	private String radius;
	
	private BigDecimal lng;
	
	private BigDecimal lat;
	
    private Date createDate;

    private Date modifyDate;
    
    @Builder.Default
    private List<DMSLocationDto> locations = new ArrayList<>();

}
