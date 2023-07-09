package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
@Table(name = "vendor")
public class Vendor extends BaseEntity {

    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Builder.Default
    @Column(name = "empty_sig", columnDefinition = "boolean default false not null")
    private Boolean emptySig = false;
}