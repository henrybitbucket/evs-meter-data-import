package com.pa.evs.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "firmware_tbl")
public class Firmware extends BaseEntity{
    
    @Column(name = "version")
    private String version;
    
    @Column(name = "hash_code")
    private String hashCode;
    
    @Column(name = "file_name")
    private String fileName;
    
}
