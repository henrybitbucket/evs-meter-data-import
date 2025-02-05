package com.pa.evs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Blob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "evs_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Report extends BaseEntity {

    private String reportName;

    @Lob
	@Column(name = "bin_Blob")
    private Blob binBlob;

}
