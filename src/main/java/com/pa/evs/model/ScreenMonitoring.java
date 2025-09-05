package com.pa.evs.model;

import com.pa.evs.enums.ScreenMonitorKey;
import com.pa.evs.enums.ScreenMonitorStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "screen_monitoring")
public class ScreenMonitoring {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "key")
    @Enumerated(EnumType.STRING)
    private ScreenMonitorKey key;

    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "value")
    private String value;
    
    @Column(name = "value_cpu")
    private Double valueCpu;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ScreenMonitorStatus status;
    
    @Column(name = "last_up_time")
    private Long lastUpTime;
    
    @Column(name = "last_down_time")
    private Long lastDownTime;
    
    @Column(name = "jan_1_value")
    private String jan1Value;
    
    @Column(name = "last_month_value")
    private String lastMonthValue;
    
    @Column(name = "last_2month_value")
    private String last2MonthValue;

}