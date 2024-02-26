package com.pa.evs.dto;

import java.math.BigDecimal;

import com.pa.evs.model.DMSLock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Data
public class DMSLockDto {

    private Integer originalId;
	
    private String lockBid;
	
    private String lockEsn;
	
    private String lockName;
	
    private String lockNumber;
	
    private boolean lastSyncExist;
	
	private DMSLockVendorDto vendor;

	public static DMSLockDto build(DMSLock li) {
		
		return DMSLockDto
				.builder()
				.lockBid(li.getLockBid())
				.lockEsn(li.getLockEsn())
				.lockName(li.getLockName())
				.lockNumber(li.getLockNumber())
				.originalId(li.getOriginalId())
				.vendor(DMSLockVendorDto.build(li.getVendor()))
				.build();
	}
	
}
