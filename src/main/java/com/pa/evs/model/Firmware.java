package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "firmware_tbl")
public class Firmware extends BaseEntity{
    
    @Column(name = "version")
    private String version;
    
    @Column(name = "hash_code")
    private String hashCode;
    
    @Column(name = "file_name")
    private String fileName;
    
	@ManyToOne
	@JoinColumn(name = "vendor_id")
	private Vendor vendor;
    
}
