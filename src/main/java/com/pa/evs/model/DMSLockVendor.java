package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pa.evs.enums.VendorType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "dms_lock_vendor")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class DMSLockVendor extends BaseEntity {

	@Column(name = "name")
    private String name;
	
	@Column(name = "label", unique = true)
    private String label;
	
	@Column(name = "company_name")
    private String companyName;
	
    @Column(name = "type")
    private VendorType type;

}
