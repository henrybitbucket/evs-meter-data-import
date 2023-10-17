package com.pa.evs.model;

import java.sql.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
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
    
    @Column(name = "signature_algorithm")
    private String signatureAlgorithm;
    
    @Column(name = "key_type")
    private String keyType;
    
    @Column(name = "key_path")
    private String keyPath;
    
	@Lob
	@Column(name = "key_content")
    private Blob keyContent;
	
    @Column(name = "certificate", length = 20000)
    private String certificate;
    
    @Column(name = "csr_path")
    private String csrPath;
    
	@Lob
	@Column(name = "csr_blob")
    private Blob csrBlob;
    
    @Builder.Default
    private Long maxMidValue = 4294967295l;
    
}