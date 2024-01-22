package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "mms_meter", uniqueConstraints = {@UniqueConstraint(columnNames = "uid"), @UniqueConstraint(columnNames = "msn")})
public class MMSMeter extends BaseEntity {

    private String uid; // MCU UID
    
    @Column(name = "msn", unique = true, nullable = false)
    private String msn;

    private String lastUid; // LAST MCU UID
    
    private String remark;
    
    private Long lastestDecoupleTime;
    
    private Long lastestCoupledTime;
    
    private String lastestDecoupleUser;
    
    private String lastestCoupledUser;
}
