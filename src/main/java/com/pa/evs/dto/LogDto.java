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
    
    private Date createDate;
    
    private String topic;
    
    private String pId;
    
    private String pType;
    
    private String raw;
    
    private String mqttAddress;
    
    private Long repStatus;
    
    private String batchId;
    
    private Integer markView;
    
    private String sn;
    
    private String address;
    
    private Long groupId;
    
    private String groupName;
    
    private String FtpResStatus;
    
    private String RepStatusDesc;
    
    private String groupRemark;
    
    private GroupDto groupDto;
    
    private String userID;
    
    private String userName;
    
    private String userEmail;
    
    private String handleSubscribeDesc;
    
    private String statusDesc;
}
