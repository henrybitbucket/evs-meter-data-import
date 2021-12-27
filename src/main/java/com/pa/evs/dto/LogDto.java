package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.dto.LogBatchDto.LogBatchDtoBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@Builder
public class LogDto {
    
    private Long id;
    
    private String type;
    
    private Long mid;
    
    private String uid;
    
    private Long oid;
    
    private String gid;
    
    private String msn;
    
    private String sig;
    
    private Long status;
    
    private String pId;
    
    private String pType;
    
    private String raw;
    
    private String mqttAddress;
    
    private String userID;
    
    private String userName;
    
    private String userEmail;
}
