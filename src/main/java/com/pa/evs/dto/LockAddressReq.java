package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LockAddressReq {
	
    private String bid;
    private String lockNumber;
	
}
