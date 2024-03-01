package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;

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
public class CaRequestLogDto {
    
    private Long id;

    private String uid;
    
    private String msn;
    
    private String sn;
    
    private String certificate;
    
    private String raw;
    
    private Long startDate;
    
    private Long endDate;
    
    private Boolean requireRefresh;
    
    private Long installer;
    
    private String installerName;
    
    private String installerEmail;
    
    private Long group;
    
	private String homeAddress;

    private Long buildingId;
    
    private Long floorLevelId;
    
    private Long buildingUnitId;
    
    private DeviceStatus status;
    
    private DeviceType type;
    
	private String coupledUser;
    
    private BuildingDto building;
    
    private FloorLevelDto floorLevel;
    
    private BuildingUnitDto buildingUnit;
    
    private AddressDto address;
    
    private Long vendor;
    
    @Builder.Default
    private List<Long> projectTags = new ArrayList<>();
    
    @Builder.Default
    private Boolean unCoupleAddress = false;
    
    private Boolean isReplaced;
    
    private String replaceReason;
    
    private Integer sendMDTToPi;
    
    private String remark;
    
    private String message;
    
    @Builder.Default
    private boolean updateMeter = false;
}
