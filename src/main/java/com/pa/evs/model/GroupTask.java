package com.pa.evs.model;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.pa.evs.enums.CommandEnum;

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
@Table(name = "group_task")
public class GroupTask extends BaseEntity {

    public enum Type {
        ONE_TIME, DAILY, WEEKLY, MONTHLY;
    }

    @Column(name = "command", length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private CommandEnum command;

    @Column(name = "type", length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "start_time")
    private Date startTime;
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

}
