package com.pa.evs.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
@Entity
@Table(name = "meter_log")
public class MeterLog extends BaseEntity {

    private String uid;
    
    private String msn;
	
    private String kwh;
    
    private String kw;
    
    private String i;
    
    private String v;
    
    private String pf;
	
	private Long dt;
	
	private Date dtd;
	
	private Integer dtn;
	
	public static MeterLog build(Map<String, Object> data) throws Exception {
		
		return builder()
				.uid((String)data.get("uid"))
				.msn((String)data.get("msn"))
				.kwh((String)data.get("kwh"))
				.kw((String)data.get("kw"))
				.i((String)data.get("i"))
				.v((String)data.get("v"))
				.pf((String)data.get("pf"))
				.dt((Long)data.get("dt"))
				.dtd((Date)data.get("dtd"))
				.dtn((Integer)data.get("dtn"))
				.build();
	}
}
