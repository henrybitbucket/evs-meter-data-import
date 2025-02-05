package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
		name = "country_code",
		indexes = {
				@Index(name = "idx_name_calling_code_country_code", columnList="name,calling_code", unique = true)
		}
)
public class CountryCode extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
    
	@Column(name = "name", nullable = false)
    private String name;
	
	@Column(name = "calling_code")
    private String callingCode;
	
	@Column(name = "iso_code")
    private String isoCode;
	
	@Column(name = "img")
    private String img;
}
