package com.pa.evs.model;

import com.pa.evs.enums.CommandEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "group_task")
public class GroupTask extends BaseEntity {

    public enum Type {
        ONE_TIME, DAILY;
    }

    @Column(name = "command", length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private CommandEnum command;

    @Column(name = "type", length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "start_time")
    private Date startTime;

}
