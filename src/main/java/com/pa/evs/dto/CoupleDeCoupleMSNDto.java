package com.pa.evs.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author tonyk
 *
 */
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoupleDeCoupleMSNDto {

	private String msn;
	private String sn;
	private String remarkForMeter;
	private String action;
	private String message;
	private int line;
	private Map<String, Integer> head;
}
